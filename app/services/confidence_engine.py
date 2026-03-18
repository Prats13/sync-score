"""
Depth-first confidence scoring engine.

Score = Evidence Strength (50%) + Specialisation (30%) + Declaration Alignment (20%)

Evidence Strength — per tag, how well proven is it:
  - present in dep file                 +1
  - multiple artifacts confirm same tag +1 per additional artifact (max +3)
  - repo recency < 180 days             +1
  - repo recency < 30 days              +1 (extra recency bonus)
  - stars >= 10                         +1
  - PDF/blog evidence                   +1
  - declared by producer too            +1
  max raw evidence per tag: 9 → normalised to 0–10

Specialisation — depth in a core domain:
  fraction of passed artifacts that share the producer's top tag
  0 artifacts → 0, all artifacts share top tag → 1.0

Declaration Alignment — honesty signal:
  declared tags that are verified / total declared tags
  undeclared verified tags score as partial (we found capability they didn't claim)
"""

from collections import Counter
from typing import Any

from app.models.capability import CapabilityDeclaration
from app.models.proof_artifact import ProofArtifact
from app.services.capability_tags import TAG_WEIGHTS


def _evidence_score_for_tag(
    tag: str,
    passed_artifacts: list[ProofArtifact],
    declared_tags: set[str],
) -> float:
    """Return a 0–10 evidence score for a single tag."""
    raw = 0

    artifacts_with_tag = [
        a for a in passed_artifacts
        if a.capability_tags and tag in a.capability_tags
    ]
    if not artifacts_with_tag:
        return 0.0

    # Base: tag is present in at least one artifact
    raw += 1

    # Multi-artifact corroboration (max +3)
    raw += min(len(artifacts_with_tag) - 1, 3)

    # Recency signals from GitHub artifacts
    github_artifacts = [
        a for a in artifacts_with_tag if a.artifact_type == "github_repo"
    ]
    if github_artifacts:
        signals = github_artifacts[0].validation_details or {}
        recency = signals.get("recency_days")
        if recency is not None:
            if recency < 180:
                raw += 1
            if recency < 30:
                raw += 1  # extra bonus for very active repos
        stars = signals.get("stars", 0) or 0
        if stars >= 10:
            raw += 1

    # PDF or blog evidence
    non_github = [
        a for a in artifacts_with_tag
        if a.artifact_type in ("pdf", "tech_blog", "case_study", "product_page")
    ]
    if non_github:
        raw += 1

    # Producer declared this capability
    if tag in declared_tags:
        raw += 1

    # Normalise: max possible raw = 9 → scale to 0–10
    return min(raw / 9.0 * 10.0, 10.0)


def compute_score(
    declaration: CapabilityDeclaration,
    artifacts: list[ProofArtifact],
) -> dict[str, Any]:
    passed_artifacts = [a for a in artifacts if a.validation_status == "passed"]

    # ── Collect verified tags ─────────────────────────────────────────────────
    all_verified_tags: list[str] = []
    for artifact in passed_artifacts:
        all_verified_tags += (artifact.capability_tags or [])

    verified_tags: set[str] = set(all_verified_tags)
    tag_frequency = Counter(all_verified_tags)  # how many artifacts confirm each tag

    # ── Collect declared tags ─────────────────────────────────────────────────
    declared_tags: set[str] = set()
    for cap in (declaration.capabilities or []):
        name = cap.get("name", "")
        if name:
            declared_tags.add(name)

    # ── 1. Evidence Strength (50% of final score) ─────────────────────────────
    # Score each verified tag, weight by TAG_WEIGHTS, sum up
    tag_evidence: dict[str, float] = {}
    weighted_evidence_sum = 0.0
    max_possible_weighted = 0.0

    for tag in verified_tags:
        evidence = _evidence_score_for_tag(tag, passed_artifacts, declared_tags)
        tag_evidence[tag] = round(evidence, 2)
        weight = TAG_WEIGHTS.get(tag, 3)
        weighted_evidence_sum += evidence * weight
        max_possible_weighted += 10.0 * weight  # max evidence score × weight

    # Normalise to 0–50
    if max_possible_weighted > 0:
        evidence_component = (weighted_evidence_sum / max_possible_weighted) * 50
    else:
        evidence_component = 0.0

    # ── 2. Specialisation (30% of final score) ────────────────────────────────
    # Find the producer's top tag by frequency across artifacts
    specialisation_score = 0.0
    top_tag = None

    if tag_frequency and passed_artifacts:
        top_tag = tag_frequency.most_common(1)[0][0]
        top_tag_count = tag_frequency.most_common(1)[0][1]

        # Fraction of passed artifacts that share the top tag
        artifacts_with_top = sum(
            1 for a in passed_artifacts
            if a.capability_tags and top_tag in a.capability_tags
        )
        specialisation_ratio = artifacts_with_top / len(passed_artifacts)

        # Depth bonus: how many deps/signals confirm the top tag
        top_evidence = tag_evidence.get(top_tag, 0)
        specialisation_score = specialisation_ratio * (top_evidence / 10.0) * 30

    # ── 3. Declaration Alignment (20% of final score) ─────────────────────────
    alignment_score = 0.0

    if declared_tags:
        # Declared + verified = full alignment
        corroborated = declared_tags & verified_tags
        alignment_ratio = len(corroborated) / len(declared_tags)
        alignment_score = alignment_ratio * 15  # max 15 from declared alignment

    # Undeclared verified tags = partial bonus (found more than they claimed)
    undeclared_verified = verified_tags - declared_tags
    if undeclared_verified and verified_tags:
        undeclared_ratio = len(undeclared_verified) / len(verified_tags)
        alignment_score += undeclared_ratio * 5  # max +5 for honest discovery

    alignment_score = min(alignment_score, 20)

    # ── Final score ───────────────────────────────────────────────────────────
    raw_score = evidence_component + specialisation_score + alignment_score
    final_score = min(round(raw_score), 100)

    # ── Tier ──────────────────────────────────────────────────────────────────
    if final_score >= 70:
        tier = "Gold"
    elif final_score >= 40:
        tier = "Silver"
    else:
        tier = "Bronze"

    corroborated = sorted(declared_tags & verified_tags)

    return {
        "score": final_score,
        "tier": tier,
        "verified_capabilities": {
            tag: {
                "evidence_score": tag_evidence.get(tag, 0),
                "artifact_count": tag_frequency.get(tag, 0),
            }
            for tag in sorted(verified_tags)
        },
        "declared_capabilities": {
            "capabilities": declaration.capabilities or [],
            "deployment_type": declaration.deployment_type,
            "frameworks": declaration.frameworks,
            "custom_description": declaration.custom_description,
        },
        "corroborated_tags": corroborated,
        "top_specialisation": top_tag,
        "score_breakdown": {
            "evidence_strength": round(evidence_component, 1),
            "specialisation": round(specialisation_score, 1),
            "declaration_alignment": round(alignment_score, 1),
        },
    }