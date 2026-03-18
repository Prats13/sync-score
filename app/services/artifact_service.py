import logging
import uuid
from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.proof_artifact import ProofArtifact
from app.models.user import User
from app.services.extraction_service import extract_from_artifact
from app.services.github_service import get_repo_signals

logger = logging.getLogger(__name__)


async def validate_artifact(artifact_id: uuid.UUID, db: AsyncSession) -> None:
    result = await db.execute(select(ProofArtifact).where(ProofArtifact.id == artifact_id))
    artifact = result.scalar_one_or_none()
    if not artifact:
        logger.warning("Artifact %s not found — skipping validation", artifact_id)
        return

    artifact.validation_status = "in_progress"
    await db.flush()
    logger.info("Validating artifact %s type=%s url=%s", artifact_id, artifact.artifact_type, artifact.url)

    try:
        if artifact.artifact_type == "github_repo":
            signals = await get_repo_signals(artifact.url)
        else:
            signals = await extract_from_artifact(artifact.artifact_type, artifact.url)

        accessible = signals.get("accessible", False)
        capability_tags = signals.pop("capability_tags", [])

        artifact.validation_status = "passed" if accessible else "failed"
        artifact.validation_details = signals
        artifact.capability_tags = capability_tags
        artifact.extraction_method = signals.get("extraction_method", "unknown")

        logger.info(
            "Artifact %s → %s | tags: %s",
            artifact_id,
            artifact.validation_status,
            capability_tags,
        )
    except Exception as e:
        logger.exception("Validation crashed for artifact %s", artifact_id)
        artifact.validation_status = "failed"
        artifact.validation_details = {"accessible": False, "error": str(e)}
        artifact.capability_tags = []

    await db.flush()
    await _check_badge_2_eligibility(artifact.user_id, db)
    await db.commit()


async def _check_badge_2_eligibility(user_id: uuid.UUID, db: AsyncSession) -> None:
    user_result = await db.execute(select(User).where(User.id == user_id))
    user = user_result.scalar_one_or_none()
    if not user or not (user.linkedin_url and user.github_url):
        return

    artifacts_result = await db.execute(
        select(ProofArtifact).where(ProofArtifact.user_id == user_id)
    )
    artifacts = artifacts_result.scalars().all()
    if not artifacts:
        return

    all_done = all(a.validation_status in ("passed", "failed") for a in artifacts)
    any_passed = any(a.validation_status == "passed" for a in artifacts)

    if all_done and any_passed:
        user.badge_2 = True
        user.is_publicly_searchable = True
        await db.flush()
