import uuid

from pydantic import BaseModel, EmailStr, field_validator


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str
    entity_type: str

    @field_validator("password")
    @classmethod
    def password_min_length(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        return v

    @field_validator("entity_type")
    @classmethod
    def entity_type_valid(cls, v: str) -> str:
        if v not in ("individual", "company"):
            raise ValueError("entity_type must be 'individual' or 'company'")
        return v


class OTPVerifyRequest(BaseModel):
    email: EmailStr
    otp: str


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class UserResponse(BaseModel):
    model_config = {"from_attributes": True}

    id: uuid.UUID
    email: str
    entity_type: str
    is_active: bool
    badge_1: bool
    badge_2: bool
    badge_3: bool
    badge_4: bool
    is_publicly_searchable: bool
    oauth_provider: str | None
    linkedin_url: str | None
    github_url: str | None
