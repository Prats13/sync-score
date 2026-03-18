import uuid
from datetime import datetime, timezone
from typing import TYPE_CHECKING

from sqlalchemy import Boolean, DateTime, Enum, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base

if TYPE_CHECKING:
    from app.models.proof_artifact import ProofArtifact
    from app.models.capability import CapabilityDeclaration, CapabilityDocument


class EntityType(str):
    individual = "individual"
    company = "company"


class OAuthProvider(str):
    google = "google"
    linkedin = "linkedin"


class User(Base):
    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    email: Mapped[str] = mapped_column(String(255), unique=True, nullable=False, index=True)
    hashed_password: Mapped[str | None] = mapped_column(String(255), nullable=True)
    entity_type: Mapped[str] = mapped_column(
        Enum("individual", "company", name="entity_type_enum"), nullable=False
    )
    is_active: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    badge_1: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    badge_2: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    badge_3: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    badge_4: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    is_publicly_searchable: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    oauth_provider: Mapped[str | None] = mapped_column(
        Enum("google", "linkedin", name="oauth_provider_enum"), nullable=True
    )
    oauth_id: Mapped[str | None] = mapped_column(String(255), nullable=True)

    linkedin_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    github_url: Mapped[str | None] = mapped_column(String(500), nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    proof_artifacts: Mapped[list["ProofArtifact"]] = relationship(
        "ProofArtifact", back_populates="user", lazy="select"
    )
    capability_declaration: Mapped["CapabilityDeclaration | None"] = relationship(
        "CapabilityDeclaration", back_populates="user", uselist=False, lazy="select"
    )
    capability_document: Mapped["CapabilityDocument | None"] = relationship(
        "CapabilityDocument", back_populates="user", uselist=False, lazy="select"
    )
