import uuid
from datetime import datetime
from typing import Any

from pydantic import BaseModel, HttpUrl, field_validator

_VALID_ARTIFACT_TYPES = {
    "github_repo",
    "live_demo",
    "product_page",
    "tech_blog",
    "sandbox_deployment",
    "case_study",
    "pdf",
}


class PresenceProfileRequest(BaseModel):
    linkedin_url: str
    github_url: str

    @field_validator("linkedin_url")
    @classmethod
    def linkedin_url_valid(cls, v: str) -> str:
        if "linkedin.com" not in v:
            raise ValueError("Must be a valid LinkedIn URL")
        return v

    @field_validator("github_url")
    @classmethod
    def github_url_valid(cls, v: str) -> str:
        if "github.com" not in v:
            raise ValueError("Must be a valid GitHub URL")
        return v


class ArtifactSubmitRequest(BaseModel):
    artifact_type: str
    url: HttpUrl

    @field_validator("artifact_type")
    @classmethod
    def artifact_type_valid(cls, v: str) -> str:
        if v not in _VALID_ARTIFACT_TYPES:
            raise ValueError(f"artifact_type must be one of {_VALID_ARTIFACT_TYPES}")
        return v


class ArtifactStatusResponse(BaseModel):
    model_config = {"from_attributes": True}

    id: uuid.UUID
    artifact_type: str
    url: str
    validation_status: str
    validation_details: dict[str, Any] | None
    capability_tags: list[str]
    extraction_method: str | None
    created_at: datetime
