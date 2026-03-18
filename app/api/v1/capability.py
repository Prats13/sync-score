from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.deps import get_db, require_badge_2
from app.models.capability import CapabilityDeclaration, CapabilityDocument
from app.models.proof_artifact import ProofArtifact
from app.models.user import User
from app.schemas.capability import (
    CapabilityDeclarationRequest,
    CapabilityDeclarationResponse,
    CapabilityDocumentPatchRequest,
    CapabilityDocumentResponse,
)
from app.services import capability_doc as cap_doc_service
from app.services.confidence_engine import compute_score

router = APIRouter(prefix="/capability", tags=["capability"])


@router.post("/declaration", response_model=CapabilityDeclarationResponse)
async def upsert_declaration(
    payload: CapabilityDeclarationRequest,
    current_user: User = Depends(require_badge_2),
    db: AsyncSession = Depends(get_db),
) -> CapabilityDeclaration:
    result = await db.execute(
        select(CapabilityDeclaration).where(CapabilityDeclaration.user_id == current_user.id)
    )
    declaration = result.scalar_one_or_none()

    if declaration is None:
        declaration = CapabilityDeclaration(user_id=current_user.id)
        db.add(declaration)

    for field, value in payload.model_dump().items():
        setattr(declaration, field, value)

    await db.flush()
    return declaration


@router.get("/declaration", response_model=CapabilityDeclarationResponse)
async def get_declaration(
    current_user: User = Depends(require_badge_2),
    db: AsyncSession = Depends(get_db),
) -> CapabilityDeclaration:
    result = await db.execute(
        select(CapabilityDeclaration).where(CapabilityDeclaration.user_id == current_user.id)
    )
    declaration = result.scalar_one_or_none()
    if not declaration:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No declaration found")
    return declaration


@router.post("/document/generate", response_model=CapabilityDocumentResponse)
async def generate_document(
    current_user: User = Depends(require_badge_2),
    db: AsyncSession = Depends(get_db),
) -> CapabilityDocument:
    decl_result = await db.execute(
        select(CapabilityDeclaration).where(CapabilityDeclaration.user_id == current_user.id)
    )
    declaration = decl_result.scalar_one_or_none()
    if not declaration:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Submit a capability declaration first",
        )

    artifacts_result = await db.execute(
        select(ProofArtifact).where(ProofArtifact.user_id == current_user.id)
    )
    artifacts = list(artifacts_result.scalars().all())

    # Attach artifacts to user for the LLM prompt builder
    current_user.proof_artifacts = artifacts

    score_result = compute_score(declaration, artifacts)

    llm_result = await cap_doc_service.generate_capability_document(
        current_user, declaration, score_result
    )

    doc_result = await db.execute(
        select(CapabilityDocument).where(CapabilityDocument.user_id == current_user.id)
    )
    doc = doc_result.scalar_one_or_none()

    if doc is None:
        doc = CapabilityDocument(
            user_id=current_user.id,
            declaration_id=declaration.id,
        )
        db.add(doc)

    doc.declaration_id = declaration.id
    doc.narrative = llm_result["narrative"]
    doc.positioning = llm_result["positioning"]
    doc.focus_industries = llm_result["focus_industries"]
    doc.verified_capabilities = score_result["verified_capabilities"]
    doc.declared_capabilities = score_result["declared_capabilities"]
    doc.confidence_score = score_result["score"]
    doc.tier = score_result["tier"]
    doc.raw_llm_output = llm_result["raw_llm_output"]

    # Store depth-scoring metadata directly on the document for API response
    # (not persisted in DB columns — returned via the score_result passthrough)
    doc._score_breakdown = score_result.get("score_breakdown")
    doc._top_specialisation = score_result.get("top_specialisation")
    doc._corroborated_tags = score_result.get("corroborated_tags", [])

    current_user.badge_3 = True
    await db.flush()

    # Attach transient fields for response serialisation
    doc.score_breakdown = score_result.get("score_breakdown")
    doc.top_specialisation = score_result.get("top_specialisation")
    doc.corroborated_tags = score_result.get("corroborated_tags", [])

    return doc


@router.get("/document", response_model=CapabilityDocumentResponse)
async def get_document(
    current_user: User = Depends(require_badge_2),
    db: AsyncSession = Depends(get_db),
) -> CapabilityDocument:
    result = await db.execute(
        select(CapabilityDocument).where(CapabilityDocument.user_id == current_user.id)
    )
    doc = result.scalar_one_or_none()
    if not doc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No capability document found")
    return doc


@router.patch("/document", response_model=CapabilityDocumentResponse)
async def patch_document(
    payload: CapabilityDocumentPatchRequest,
    current_user: User = Depends(require_badge_2),
    db: AsyncSession = Depends(get_db),
) -> CapabilityDocument:
    result = await db.execute(
        select(CapabilityDocument).where(CapabilityDocument.user_id == current_user.id)
    )
    doc = result.scalar_one_or_none()
    if not doc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No capability document found")

    if payload.narrative is not None:
        doc.narrative = payload.narrative
    if payload.positioning is not None:
        doc.positioning = payload.positioning
    if payload.focus_industries is not None:
        doc.focus_industries = payload.focus_industries

    await db.flush()
    return doc
