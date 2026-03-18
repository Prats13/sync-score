import logging
import uuid

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status

logger = logging.getLogger(__name__)
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import AsyncSessionLocal
from app.deps import get_db, require_badge_1
from app.models.proof_artifact import ProofArtifact
from app.models.user import User
from app.schemas.presence import ArtifactStatusResponse, ArtifactSubmitRequest, PresenceProfileRequest
from app.services import artifact_service

router = APIRouter(prefix="/presence", tags=["presence"])


@router.post("/profiles", response_model=dict)
async def save_presence_profiles(
    payload: PresenceProfileRequest,
    current_user: User = Depends(require_badge_1),
    db: AsyncSession = Depends(get_db),
) -> dict:
    current_user.linkedin_url = payload.linkedin_url
    current_user.github_url = payload.github_url
    return {"linkedin_url": current_user.linkedin_url, "github_url": current_user.github_url}


@router.post("/artifacts", response_model=ArtifactStatusResponse, status_code=status.HTTP_201_CREATED)
async def submit_artifact(
    payload: ArtifactSubmitRequest,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(require_badge_1),
    db: AsyncSession = Depends(get_db),
) -> ProofArtifact:
    artifact = ProofArtifact(
        user_id=current_user.id,
        artifact_type=payload.artifact_type,
        url=str(payload.url),
        validation_status="pending",
    )
    db.add(artifact)
    await db.flush()
    await db.commit()  # commit before background task so it can find the artifact

    artifact_id = artifact.id
    background_tasks.add_task(_run_validation, artifact_id)

    return artifact


async def _run_validation(artifact_id: uuid.UUID) -> None:
    logger.info("Background validation started for artifact %s", artifact_id)
    try:
        async with AsyncSessionLocal() as db:
            await artifact_service.validate_artifact(artifact_id, db)
        logger.info("Background validation completed for artifact %s", artifact_id)
    except Exception as e:
        logger.exception("Background validation crashed for artifact %s: %s", artifact_id, e)


@router.get("/artifacts", response_model=list[ArtifactStatusResponse])
async def list_artifacts(
    current_user: User = Depends(require_badge_1),
    db: AsyncSession = Depends(get_db),
) -> list[ProofArtifact]:
    result = await db.execute(
        select(ProofArtifact).where(ProofArtifact.user_id == current_user.id)
    )
    return list(result.scalars().all())


@router.get("/artifacts/{artifact_id}/status", response_model=ArtifactStatusResponse)
async def get_artifact_status(
    artifact_id: uuid.UUID,
    current_user: User = Depends(require_badge_1),
    db: AsyncSession = Depends(get_db),
) -> ProofArtifact:
    result = await db.execute(
        select(ProofArtifact).where(
            ProofArtifact.id == artifact_id,
            ProofArtifact.user_id == current_user.id,
        )
    )
    artifact = result.scalar_one_or_none()
    if not artifact:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Artifact not found")
    return artifact


@router.post("/verify", response_model=dict)
async def verify_badge_2(
    current_user: User = Depends(require_badge_1),
    db: AsyncSession = Depends(get_db),
) -> dict:
    from app.services.artifact_service import _check_badge_2_eligibility
    await _check_badge_2_eligibility(current_user.id, db)
    await db.refresh(current_user)
    return {"badge_2": current_user.badge_2, "is_publicly_searchable": current_user.is_publicly_searchable}
