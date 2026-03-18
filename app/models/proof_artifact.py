import uuid
from datetime import datetime, timezone
from typing import TYPE_CHECKING, Any

from sqlalchemy import DateTime, Enum, ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.types import JSON

from app.core.database import Base

if TYPE_CHECKING:
    from app.models.user import User


class ProofArtifact(Base):
    __tablename__ = "proof_artifacts"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    artifact_type: Mapped[str] = mapped_column(
        Enum(
            "github_repo",
            "live_demo",
            "product_page",
            "tech_blog",
            "sandbox_deployment",
            "case_study",
            "pdf",
            name="artifact_type_enum",
        ),
        nullable=False,
    )
    url: Mapped[str] = mapped_column(String(2000), nullable=False)

    validation_status: Mapped[str] = mapped_column(
        Enum("pending", "in_progress", "passed", "failed", name="validation_status_enum"),
        default="pending",
        nullable=False,
    )
    validation_details: Mapped[dict[str, Any] | None] = mapped_column(JSON, nullable=True)

    # Extraction output
    capability_tags: Mapped[list[str]] = mapped_column(JSON, default=list, nullable=False)
    extraction_method: Mapped[str | None] = mapped_column(String(100), nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    user: Mapped["User"] = relationship("User", back_populates="proof_artifacts")
