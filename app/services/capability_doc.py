import json
import logging
import re
from typing import Any

import anthropic

from app.core.config import settings
from app.models.capability import CapabilityDeclaration
from app.models.user import User

logger = logging.getLogger(__name__)

_SYSTEM_PROMPT = (
    "You are Syncscore's capability document generator. Generate a structured "
    "professional profile for an AI service producer based on verified signals. "
    "Always respond with valid JSON only — no markdown, no prose outside the JSON object."
)


def _build_user_prompt(
    user: User,
    declaration: CapabilityDeclaration,
    score_result: dict[str, Any],
    artifact_summaries: list[dict[str, Any]],
) -> str:
    verified = score_result.get("verified_capabilities", {})
    declared = score_result.get("declared_capabilities", {})
    corroborated = score_result.get("corroborated_tags", [])

    artifact_lines = []
    for a in artifact_summaries:
        tags = ", ".join(a.get("tags", [])) or "none detected"
        artifact_lines.append(f"  - [{a['type']}] {a['url']} → tags: {tags}")

    return f"""Producer profile to document:

Entity type: {user.entity_type}
Deployment: {declared.get('deployment_type', 'unknown')}
Frameworks: {', '.join(declared.get('frameworks', [])) or 'not specified'}

Declared capabilities:
{json.dumps(declared.get('capabilities', []), indent=2)}

Custom description from producer:
{declared.get('custom_description') or 'Not provided'}

Verified capability tags (extracted from artifacts):
{', '.join(verified.keys()) or 'none'}

Corroborated (declared AND verified):
{', '.join(corroborated) or 'none'}

Confidence score: {score_result['score']}/100  |  Tier: {score_result['tier']}

Proof artifacts:
{chr(10).join(artifact_lines) or '  none'}

Generate a JSON response with exactly these keys:
"narrative" (2-3 paragraphs: what they build, how they build it, what makes them credible),
"positioning" (1 sentence: their market position),
"focus_industries" (list of 3-5 industries most relevant to their capabilities)"""


async def generate_capability_document(
    user: User,
    declaration: CapabilityDeclaration,
    score_result: dict[str, Any],
) -> dict[str, Any]:
    artifact_summaries = []
    for artifact in user.proof_artifacts:
        if artifact.validation_status == "passed":
            artifact_summaries.append({
                "type": artifact.artifact_type,
                "url": artifact.url,
                "tags": artifact.capability_tags or [],
            })

    user_prompt = _build_user_prompt(user, declaration, score_result, artifact_summaries)

    client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
    message = await client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=1024,
        system=_SYSTEM_PROMPT,
        messages=[{"role": "user", "content": user_prompt}],
    )

    raw_output = message.content[0].text

    try:
        raw_clean = re.sub(r"^```[a-z]*\n?", "", raw_output.strip())
        raw_clean = re.sub(r"\n?```$", "", raw_clean)
        parsed = json.loads(raw_clean)
    except json.JSONDecodeError:
        json_match = re.search(r"\{.*\}", raw_output, re.DOTALL)
        if json_match:
            parsed = json.loads(json_match.group())
        else:
            logger.error("Failed to parse LLM JSON output: %s", raw_output)
            parsed = {
                "narrative": raw_output,
                "positioning": "AI service producer",
                "focus_industries": [],
            }

    return {
        "narrative": parsed.get("narrative", ""),
        "positioning": parsed.get("positioning", ""),
        "focus_industries": parsed.get("focus_industries", []),
        "raw_llm_output": raw_output,
    }
