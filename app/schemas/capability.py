import uuid
from datetime import datetime
from typing import Any

from pydantic import BaseModel, field_validator

from app.services.capability_tags import ALL_TAGS


class CapabilityItem(BaseModel):
    name: str
    detail: str | None = None

    @field_validator("name")
    @classmethod
    def name_must_be_valid_tag(cls, v: str) -> str:
        if v not in ALL_TAGS:
            raise ValueError(f"Capability name must be one of: {', '.join(ALL_TAGS)}")
        return v


class CapabilityDeclarationRequest(BaseModel):
    capabilities: list[CapabilityItem]
    deployment_type: str
    frameworks: list[str] = []
    custom_description: str | None = None

    @field_validator("deployment_type")
    @classmethod
    def deployment_type_valid(cls, v: str) -> str:
        if v not in ("cloud", "on_prem", "hybrid"):
            raise ValueError("deployment_type must be 'cloud', 'on_prem', or 'hybrid'")
        return v


class CapabilityDeclarationResponse(BaseModel):
    model_config = {"from_attributes": True}

    id: uuid.UUID
    user_id: uuid.UUID
    capabilities: list[dict[str, Any]]
    deployment_type: str
    frameworks: list[str]
    custom_description: str | None
    created_at: datetime
    updated_at: datetime


class CapabilityDocumentPatchRequest(BaseModel):
    """Only producer-editable fields — system fields are intentionally absent."""
    model_config = {"extra": "forbid"}

    narrative: str | None = None
    positioning: str | None = None
    focus_industries: list[str] | None = None


class CapabilityDocumentResponse(BaseModel):
    model_config = {"from_attributes": True}

    id: uuid.UUID
    user_id: uuid.UUID
    declaration_id: uuid.UUID
    narrative: str | None
    positioning: str | None
    focus_industries: list[str]
    verified_capabilities: dict[str, Any]
    declared_capabilities: dict[str, Any]
    confidence_score: int
    tier: str
    execution_verified: bool
    score_breakdown: dict[str, Any] | None = None
    top_specialisation: str | None = None
    corroborated_tags: list[str] = []
    created_at: datetime
    updated_at: datetime
