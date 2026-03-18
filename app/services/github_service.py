"""
GitHub extraction pipeline — Tree API first approach.

Flow:
1. Repo metadata (stars, language, last push, topics)
2. Full repo tree via GET /git/trees/{branch}?recursive=1  (single API call)
3. From the tree, identify and fetch:
   - Dependency files (requirements.txt, pyproject.toml, package.json, etc.) at ANY depth
   - Jupyter notebooks (up to 3, parse imports)
   - PDFs (download + pdfplumber + Claude)
   - README.md
   - Dockerfile / docker-compose.yml
4. Map everything → capability_tags
5. README → Claude Haiku for additional tag extraction
"""

import asyncio
import base64
import io
import json
import logging
import re
import tomllib
from datetime import datetime, timezone
from typing import Any

import httpx

from app.core.config import settings
from app.services.capability_tags import JS_DEP_TAGS, PYTHON_DEP_TAGS, deps_to_tags

logger = logging.getLogger(__name__)

# Files we care about — matched against the last path segment
_DEP_FILENAMES = {
    "requirements.txt",
    "pyproject.toml",
    "setup.cfg",
    "setup.py",
    "Pipfile",
    "package.json",
}

_MAX_NOTEBOOKS = 3
_MAX_PDFS = 2


# ── Parsers ───────────────────────────────────────────────────────────────────

def _parse_requirements_txt(content: str) -> list[str]:
    packages = []
    for line in content.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("-"):
            continue
        pkg = re.split(r"[>=<!~\[\s@]", line)[0].strip().lower()
        if pkg:
            packages.append(pkg)
    return packages


def _parse_pyproject_toml(content: str) -> list[str]:
    try:
        data = tomllib.loads(content)
    except Exception:
        return []
    packages = []
    deps = data.get("project", {}).get("dependencies", [])
    packages += [re.split(r"[>=<!~\[\s@]", d)[0].strip().lower() for d in deps if isinstance(d, str)]
    tool_poetry = data.get("tool", {}).get("poetry", {}).get("dependencies", {})
    packages += [k.lower() for k in tool_poetry if k.lower() != "python"]
    return packages


def _parse_setup_cfg(content: str) -> list[str]:
    packages = []
    in_deps = False
    for line in content.splitlines():
        if "install_requires" in line:
            in_deps = True
            continue
        if in_deps:
            stripped = line.strip()
            if not stripped:
                break
            if not (line.startswith(" ") or line.startswith("\t")):
                break
            pkg = re.split(r"[>=<!~\[\s@]", stripped)[0].strip().lower()
            if pkg:
                packages.append(pkg)
    return packages


def _parse_pipfile(content: str) -> list[str]:
    packages = []
    in_packages = False
    for line in content.splitlines():
        if line.strip() in ("[packages]", "[dev-packages]"):
            in_packages = True
            continue
        if line.strip().startswith("[") and in_packages:
            in_packages = False
        if in_packages and "=" in line:
            pkg = line.split("=")[0].strip().lower()
            if pkg:
                packages.append(pkg)
    return packages


def _parse_package_json(content: str) -> list[str]:
    try:
        data = json.loads(content)
        deps: dict = {}
        deps.update(data.get("dependencies", {}))
        deps.update(data.get("devDependencies", {}))
        return [k.lower() for k in deps]
    except Exception:
        return []


def _parse_notebook_imports(content: str) -> list[str]:
    """Extract import statements from a Jupyter notebook's code cells."""
    try:
        nb = json.loads(content)
    except Exception:
        return []
    packages = []
    for cell in nb.get("cells", []):
        if cell.get("cell_type") != "code":
            continue
        source = "".join(cell.get("source", []))
        for match in re.finditer(r"^(?:import|from)\s+([\w]+)", source, re.MULTILINE):
            pkg = match.group(1).lower().strip()
            # Map common import names to package names
            pkg = _normalize_import(pkg)
            if pkg:
                packages.append(pkg)
    return packages


def _normalize_import(import_name: str) -> str:
    """Map Python import name to pip package name where they differ."""
    _IMPORT_TO_PKG = {
        "cv2": "opencv-python",
        "PIL": "pillow",
        "pil": "pillow",
        "sklearn": "scikit-learn",
        "serial": "pyserial",
        "bs4": "beautifulsoup4",
        "dotenv": "python-dotenv",
        "yaml": "pyyaml",
        "torch": "torch",
        "tf": "tensorflow",
    }
    return _IMPORT_TO_PKG.get(import_name, import_name)


def _parse_dockerfile(content: str) -> list[str]:
    """Extract pip installs from Dockerfile."""
    packages = []
    for line in content.splitlines():
        if "pip install" in line:
            pkgs = re.findall(r"pip install\s+(.*?)(?:\\|$)", line)
            for pkg_str in pkgs:
                for pkg in pkg_str.split():
                    clean = re.split(r"[>=<!~\[]", pkg)[0].strip().lower()
                    if clean and not clean.startswith("-"):
                        packages.append(clean)
    return packages


# ── GitHub API helpers ────────────────────────────────────────────────────────

def _parse_owner_repo(github_url: str) -> tuple[str, str] | None:
    pattern = r"github\.com/([^/]+)/([^/\s?#]+)"
    match = re.search(pattern, github_url)
    if not match:
        return None
    return match.group(1), match.group(2).rstrip(".git")


def _decode_content(file_data: dict) -> str | None:
    try:
        return base64.b64decode(file_data.get("content", "")).decode("utf-8", errors="ignore")
    except Exception:
        return None


async def _fetch_file_by_path(
    client: httpx.AsyncClient,
    owner: str,
    repo: str,
    path: str,
    headers: dict,
) -> str | None:
    try:
        resp = await client.get(
            f"https://api.github.com/repos/{owner}/{repo}/contents/{path}",
            headers=headers,
            timeout=10.0,
        )
        if resp.status_code == 200:
            return _decode_content(resp.json())
    except Exception as e:
        logger.debug("Failed to fetch %s: %s", path, e)
    return None


async def _fetch_pdf_bytes(client: httpx.AsyncClient, url: str) -> bytes | None:
    try:
        resp = await client.get(url, timeout=15.0, follow_redirects=True)
        if resp.status_code == 200:
            return resp.content
    except Exception as e:
        logger.debug("Failed to fetch PDF %s: %s", url, e)
    return None


async def _extract_pdf_tags(pdf_bytes: bytes) -> tuple[list[str], str]:
    """pdfplumber → text → Claude Haiku → tags."""
    import pdfplumber
    from app.services.capability_tags import ALL_TAGS

    try:
        with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
            pages_text = [page.extract_text() or "" for page in pdf.pages[:10]]
        text = re.sub(r"\s+", " ", " ".join(pages_text))[:5000]
        logger.info("PDF extracted %d chars from %d pages", len(text), len(pages_text))
    except Exception as e:
        logger.warning("PDF parse failed: %s", e)
        return [], ""

    if not text.strip():
        logger.warning("PDF text extraction returned empty content")
        return [], ""

    if not settings.ANTHROPIC_API_KEY:
        logger.warning("No ANTHROPIC_API_KEY — skipping PDF Claude extraction")
        return [], ""

    import anthropic as anthropic_sdk

    try:
        ac = anthropic_sdk.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        msg = await ac.messages.create(
            model="claude-haiku-4-5-20251001",
            max_tokens=256,
            messages=[{
                "role": "user",
                "content": (
                    f"Extract AI/ML capability signals from this document. Return JSON only.\n\n"
                    f"Text: {text}\n\n"
                    f"Valid tags: {', '.join(ALL_TAGS)}\n\n"
                    f'Return: {{"capability_tags": ["tag1", ...], "summary": "one sentence"}}'
                ),
            }],
        )
        raw = re.sub(r"^```[a-z]*\n?", "", msg.content[0].text.strip())
        raw = re.sub(r"\n?```$", "", raw)
        parsed = json.loads(raw)
        tags = [t for t in parsed.get("capability_tags", []) if t in ALL_TAGS]
        return tags, parsed.get("summary", "")
    except Exception as e:
        logger.warning("PDF Claude extraction failed: %s", e)
        return [], ""


async def _extract_readme_tags(
    client: httpx.AsyncClient,
    owner: str,
    repo: str,
    headers: dict,
) -> tuple[list[str], str]:
    from app.services.capability_tags import ALL_TAGS

    content = await _fetch_file_by_path(client, owner, repo, "README.md", headers)
    if not content or len(content) < 100 or not settings.ANTHROPIC_API_KEY:
        return [], ""

    import anthropic as anthropic_sdk

    try:
        ac = anthropic_sdk.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        msg = await ac.messages.create(
            model="claude-haiku-4-5-20251001",
            max_tokens=256,
            messages=[{
                "role": "user",
                "content": (
                    f"Extract AI/ML capability signals from this README. Return JSON only.\n\n"
                    f"README:\n{content[:4000]}\n\n"
                    f"Valid tags: {', '.join(ALL_TAGS)}\n\n"
                    f'Return: {{"capability_tags": ["tag1", ...], "summary": "one sentence"}}'
                ),
            }],
        )
        raw = re.sub(r"^```[a-z]*\n?", "", msg.content[0].text.strip())
        raw = re.sub(r"\n?```$", "", raw)
        parsed = json.loads(raw)
        tags = [t for t in parsed.get("capability_tags", []) if t in ALL_TAGS]
        return tags, parsed.get("summary", "")
    except Exception as e:
        logger.warning("README Claude extraction failed: %s", e)
        return [], ""


# ── Main entry point ──────────────────────────────────────────────────────────

async def get_repo_signals(github_url: str) -> dict[str, Any]:
    parsed = _parse_owner_repo(github_url)
    if not parsed:
        return {"accessible": False, "error": "Could not parse GitHub URL"}

    owner, repo = parsed
    headers = {"Accept": "application/vnd.github+json"}
    if settings.GITHUB_TOKEN:
        headers["Authorization"] = f"token {settings.GITHUB_TOKEN}"

    async with httpx.AsyncClient(timeout=15.0) as client:
        # ── 1. Repo metadata ──────────────────────────────────────────────────
        try:
            meta_resp = await client.get(
                f"https://api.github.com/repos/{owner}/{repo}",
                headers=headers,
            )
            if meta_resp.status_code == 404:
                return {"accessible": False, "error": "Repository not found"}
            meta_resp.raise_for_status()
            meta = meta_resp.json()
        except httpx.HTTPError as e:
            return {"accessible": False, "error": str(e)}

        default_branch = meta.get("default_branch", "main")
        last_push = meta.get("pushed_at")
        recency_days: int | None = None
        if last_push:
            pushed_dt = datetime.fromisoformat(last_push.replace("Z", "+00:00"))
            recency_days = (datetime.now(timezone.utc) - pushed_dt).days

        # ── 2. Full repo tree (single API call) ───────────────────────────────
        tree_items: list[dict] = []
        try:
            tree_resp = await client.get(
                f"https://api.github.com/repos/{owner}/{repo}/git/trees/{default_branch}?recursive=1",
                headers=headers,
                timeout=15.0,
            )
            if tree_resp.status_code == 200:
                tree_data = tree_resp.json()
                tree_items = [
                    item for item in tree_data.get("tree", [])
                    if item.get("type") == "blob"
                ]
                if tree_data.get("truncated"):
                    logger.warning("Tree truncated for %s/%s — large repo", owner, repo)
        except Exception as e:
            logger.warning("Tree fetch failed for %s/%s: %s", owner, repo, e)

        # ── 3. Classify files from tree ───────────────────────────────────────
        dep_file_paths: list[str] = []
        notebook_paths: list[str] = []
        pdf_paths: list[str] = []
        has_dockerfile = False

        for item in tree_items:
            path = item.get("path", "")
            filename = path.split("/")[-1]

            if filename in _DEP_FILENAMES:
                dep_file_paths.append(path)
            elif filename.endswith(".ipynb"):
                notebook_paths.append(path)
            elif filename.lower().endswith(".pdf"):
                pdf_paths.append(path)
            elif filename in ("Dockerfile", "docker-compose.yml", "docker-compose.yaml"):
                has_dockerfile = True

        logger.info(
            "%s/%s tree: %d dep files, %d notebooks, %d PDFs",
            owner, repo,
            len(dep_file_paths), len(notebook_paths), len(pdf_paths),
        )

        # ── 4. Fetch dep files in parallel ────────────────────────────────────
        dep_contents = await asyncio.gather(*[
            _fetch_file_by_path(client, owner, repo, p, headers)
            for p in dep_file_paths
        ])

        # ── 5. Fetch notebooks (limited) ──────────────────────────────────────
        nb_contents = await asyncio.gather(*[
            _fetch_file_by_path(client, owner, repo, p, headers)
            for p in notebook_paths[:_MAX_NOTEBOOKS]
        ])

        # ── 6. Fetch PDFs (limited) ───────────────────────────────────────────
        pdf_raw_urls = [
            f"https://raw.githubusercontent.com/{owner}/{repo}/{default_branch}/{p}"
            for p in pdf_paths[:_MAX_PDFS]
        ]
        pdf_bytes_list = await asyncio.gather(*[
            _fetch_pdf_bytes(client, url) for url in pdf_raw_urls
        ])

        # ── 7. README tags ────────────────────────────────────────────────────
        readme_tags, readme_summary = await _extract_readme_tags(client, owner, repo, headers)

    # ── 8. Parse dependencies ─────────────────────────────────────────────────
    all_packages: list[str] = []
    dep_files_found: list[str] = []

    for path, content in zip(dep_file_paths, dep_contents):
        if not content:
            continue
        filename = path.split("/")[-1]
        dep_files_found.append(path)

        if filename == "requirements.txt":
            all_packages += _parse_requirements_txt(content)
        elif filename == "pyproject.toml":
            all_packages += _parse_pyproject_toml(content)
        elif filename == "setup.cfg":
            all_packages += _parse_setup_cfg(content)
        elif filename == "Pipfile":
            all_packages += _parse_pipfile(content)
        elif filename == "package.json":
            all_packages += _parse_package_json(content)

    # ── 9. Parse notebook imports ─────────────────────────────────────────────
    nb_packages: list[str] = []
    for content in nb_contents:
        if content:
            nb_packages += _parse_notebook_imports(content)

    all_packages += nb_packages

    # ── 10. Map packages → tags ───────────────────────────────────────────────
    dep_tags = deps_to_tags(all_packages, {**PYTHON_DEP_TAGS, **JS_DEP_TAGS})

    # ── 11. Extract PDF tags ──────────────────────────────────────────────────
    pdf_tags: list[str] = []
    pdf_summaries: list[str] = []
    for pdf_bytes in pdf_bytes_list:
        if pdf_bytes:
            tags, summary = await _extract_pdf_tags(pdf_bytes)
            pdf_tags += tags
            if summary:
                pdf_summaries.append(summary)

    # ── 12. Dockerfile tags ───────────────────────────────────────────────────
    docker_tags = ["deployment"] if has_dockerfile else []

    # ── 13. Merge all tags ────────────────────────────────────────────────────
    all_tags = sorted(set(dep_tags) | set(readme_tags) | set(pdf_tags) | set(docker_tags))

    extraction_methods = ["github_api+tree"]
    if dep_files_found:
        extraction_methods.append("dep_parser")
    if nb_packages:
        extraction_methods.append("notebook_import_scan")
    if pdf_tags:
        extraction_methods.append("pdf_llm")
    if readme_tags:
        extraction_methods.append("readme_llm")

    return {
        "accessible": True,
        "stars": meta.get("stargazers_count", 0),
        "last_push_date": last_push,
        "recency_days": recency_days,
        "language": meta.get("language"),
        "description": meta.get("description", ""),
        "topics": meta.get("topics", []),
        "dep_files_found": dep_files_found,
        "packages_found": sorted(set(all_packages)),
        "notebooks_scanned": len([c for c in nb_contents if c]),
        "pdfs_scanned": len([b for b in pdf_bytes_list if b]),
        "pdf_summaries": pdf_summaries,
        "readme_summary": readme_summary,
        "capability_tags": all_tags,
        "coherence_note": f"Extracted via {' + '.join(extraction_methods)}",
        "extraction_method": "+".join(extraction_methods),
    }
