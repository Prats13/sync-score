import uuid
from datetime import datetime, timezone
from typing import TYPE_CHECKING, Any

from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.types import JSON

from app.core.database import Base

if TYPE_CHECKING:
    from app.models.user import User


class CapabilityDeclaration(Base):
    __tablename__ = "capability_declarations"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False
    )

    # Flexible capabilities list: [{"name": "voice_ai", "detail": "..."}, ...]
    capabilities: Mapped[list[dict[str, Any]]] = mapped_column(JSON, default=list, nullable=False)

    deployment_type: Mapped[str] = mapped_column(
        Enum("cloud", "on_prem", "hybrid", name="deployment_type_enum"), nullable=False
    )
    frameworks: Mapped[list[str]] = mapped_column(JSON, default=list, nullable=False)
    custom_description: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    user: Mapped["User"] = relationship("User", back_populates="capability_declaration")
    capability_document: Mapped["CapabilityDocument | None"] = relationship(
        "CapabilityDocument", back_populates="declaration", uselist=False
    )


class CapabilityDocument(Base):
    __tablename__ = "capability_documents"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    declaration_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("capability_declarations.id", ondelete="CASCADE"), nullable=False
    )

    # Producer-editable
    narrative: Mapped[str | None] = mapped_column(Text, nullable=True)
    positioning: Mapped[str | None] = mapped_column(Text, nullable=True)
    focus_industries: Mapped[list[str]] = mapped_column(JSON, default=list, nullable=False)

    # System-controlled
    verified_capabilities: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict, nullable=False)
    declared_capabilities: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict, nullable=False)
    confidence_score: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    tier: Mapped[str] = mapped_column(
        Enum("Bronze", "Silver", "Gold", name="tier_enum"), default="Bronze", nullable=False
    )
    execution_verified: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    raw_llm_output: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    user: Mapped["User"] = relationship("User", back_populates="capability_document")
    declaration: Mapped["CapabilityDeclaration"] = relationship(
        "CapabilityDeclaration", back_populates="capability_document"
    )
