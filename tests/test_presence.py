import uuid
from unittest.mock import AsyncMock, patch

import pytest
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.proof_artifact import ProofArtifact
from app.models.user import User


async def _create_active_user_with_badge1(client: AsyncClient, email: str) -> str:
    """Helper: register + OTP verify → return access token."""
    with patch("app.services.otp_service.get_redis") as mock_redis:
        redis_mock = AsyncMock()
        redis_mock.setex = AsyncMock()
        mock_redis.return_value = redis_mock
        await client.post(
            "/api/v1/auth/register",
            json={"email": email, "password": "password123", "entity_type": "individual"},
        )

    with patch("app.services.otp_service.get_redis") as mock_redis:
        from passlib.context import CryptContext
        ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")
        redis_mock = AsyncMock()
        redis_mock.get = AsyncMock(return_value=ctx.hash("654321"))
        redis_mock.delete = AsyncMock()
        mock_redis.return_value = redis_mock
        resp = await client.post(
            "/api/v1/auth/verify-otp", json={"email": email, "otp": "654321"}
        )

    return resp.json()["access_token"]


@pytest.mark.asyncio
async def test_submit_presence_profiles(client: AsyncClient):
    token = await _create_active_user_with_badge1(client, "presence1@example.com")
    resp = await client.post(
        "/api/v1/presence/profiles",
        json={"linkedin_url": "https://linkedin.com/in/testuser", "github_url": "https://github.com/testuser"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert "linkedin.com" in data["linkedin_url"]
    assert "github.com" in data["github_url"]


@pytest.mark.asyncio
async def test_submit_artifact_returns_pending(client: AsyncClient):
    token = await _create_active_user_with_badge1(client, "presence2@example.com")

    with patch("app.api.v1.presence._run_validation", new_callable=AsyncMock):
        resp = await client.post(
            "/api/v1/presence/artifacts",
            json={"artifact_type": "github_repo", "url": "https://github.com/openai/openai-python"},
            headers={"Authorization": f"Bearer {token}"},
        )

    assert resp.status_code == 201
    data = resp.json()
    assert data["validation_status"] == "pending"
    assert data["artifact_type"] == "github_repo"


@pytest.mark.asyncio
async def test_badge_2_set_after_artifact_passes(client: AsyncClient, db_session: AsyncSession):
    email = "presence3@example.com"
    token = await _create_active_user_with_badge1(client, email)

    # Set profile URLs
    await client.post(
        "/api/v1/presence/profiles",
        json={"linkedin_url": "https://linkedin.com/in/p3", "github_url": "https://github.com/p3"},
        headers={"Authorization": f"Bearer {token}"},
    )

    with patch("app.api.v1.presence._run_validation", new_callable=AsyncMock):
        artifact_resp = await client.post(
            "/api/v1/presence/artifacts",
            json={"artifact_type": "github_repo", "url": "https://github.com/openai/openai-python"},
            headers={"Authorization": f"Bearer {token}"},
        )

    artifact_id = artifact_resp.json()["id"]

    # Simulate artifact passing
    result = await db_session.execute(select(ProofArtifact).where(ProofArtifact.id == artifact_id))
    artifact = result.scalar_one()
    artifact.validation_status = "passed"
    artifact.validation_details = {"accessible": True}
    await db_session.commit()

    # Trigger badge 2 check
    resp = await client.post(
        "/api/v1/presence/verify",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200
    assert resp.json()["badge_2"] is True
