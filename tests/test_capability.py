from unittest.mock import AsyncMock, patch

import pytest
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.proof_artifact import ProofArtifact
from app.models.user import User


async def _create_badge2_user(client: AsyncClient, email: str, db_session: AsyncSession) -> str:
    """Helper: create user with badge_1 + badge_2."""
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
        redis_mock.get = AsyncMock(return_value=ctx.hash("111111"))
        redis_mock.delete = AsyncMock()
        mock_redis.return_value = redis_mock
        resp = await client.post(
            "/api/v1/auth/verify-otp", json={"email": email, "otp": "111111"}
        )
    token = resp.json()["access_token"]

    # Set badge_2 directly in DB
    result = await db_session.execute(select(User).where(User.email == email))
    user = result.scalar_one()
    user.badge_2 = True
    user.linkedin_url = "https://linkedin.com/in/cap"
    user.github_url = "https://github.com/cap"
    await db_session.commit()

    return token


@pytest.mark.asyncio
async def test_submit_declaration(client: AsyncClient, db_session: AsyncSession):
    token = await _create_badge2_user(client, "cap1@example.com", db_session)

    resp = await client.post(
        "/api/v1/capability/declaration",
        json={
            "rag_capability": True,
            "rag_detail": "Uses FAISS for vector search",
            "agent_systems": True,
            "agent_systems_detail": "LangGraph agents",
            "tool_orchestration": False,
            "memory_systems": False,
            "deployment_type": "cloud",
            "frameworks": ["LangChain", "FastAPI"],
        },
        headers={"Authorization": f"Bearer {token}"},
    )

    assert resp.status_code == 200
    data = resp.json()
    assert data["rag_capability"] is True
    assert data["deployment_type"] == "cloud"
    assert "LangChain" in data["frameworks"]


@pytest.mark.asyncio
async def test_generate_document_sets_badge_3(client: AsyncClient, db_session: AsyncSession):
    token = await _create_badge2_user(client, "cap2@example.com", db_session)

    await client.post(
        "/api/v1/capability/declaration",
        json={
            "rag_capability": True,
            "agent_systems": False,
            "tool_orchestration": False,
            "memory_systems": False,
            "deployment_type": "cloud",
            "frameworks": ["LangChain", "FastAPI"],
        },
        headers={"Authorization": f"Bearer {token}"},
    )

    mock_llm_result = {
        "narrative": "Test narrative paragraph.",
        "positioning": "Leading AI service provider.",
        "focus_industries": ["Finance", "Healthcare"],
        "raw_llm_output": '{"narrative": "Test", "positioning": "Leading", "focus_industries": ["Finance"]}',
    }

    with patch("app.api.v1.capability.cap_doc_service.generate_capability_document", new_callable=AsyncMock) as mock_gen:
        mock_gen.return_value = mock_llm_result
        resp = await client.post(
            "/api/v1/capability/document/generate",
            headers={"Authorization": f"Bearer {token}"},
        )

    assert resp.status_code == 200
    data = resp.json()
    assert data["narrative"] == "Test narrative paragraph."
    assert data["tier"] in ("Bronze", "Silver", "Gold")
    assert isinstance(data["confidence_score"], int)

    result = await db_session.execute(select(User).where(User.email == "cap2@example.com"))
    user = result.scalar_one()
    assert user.badge_3 is True


@pytest.mark.asyncio
async def test_patch_document_narrative(client: AsyncClient, db_session: AsyncSession):
    token = await _create_badge2_user(client, "cap3@example.com", db_session)

    await client.post(
        "/api/v1/capability/declaration",
        json={
            "rag_capability": False,
            "agent_systems": False,
            "tool_orchestration": False,
            "memory_systems": False,
            "deployment_type": "hybrid",
            "frameworks": ["FastAPI"],
        },
        headers={"Authorization": f"Bearer {token}"},
    )

    mock_llm_result = {
        "narrative": "Original narrative.",
        "positioning": "Original positioning.",
        "focus_industries": ["Tech"],
        "raw_llm_output": "{}",
    }

    with patch("app.api.v1.capability.cap_doc_service.generate_capability_document", new_callable=AsyncMock) as mock_gen:
        mock_gen.return_value = mock_llm_result
        await client.post(
            "/api/v1/capability/document/generate",
            headers={"Authorization": f"Bearer {token}"},
        )

    patch_resp = await client.patch(
        "/api/v1/capability/document",
        json={"narrative": "Updated narrative by producer."},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert patch_resp.status_code == 200
    assert patch_resp.json()["narrative"] == "Updated narrative by producer."


@pytest.mark.asyncio
async def test_patch_document_rejects_confidence_score(client: AsyncClient, db_session: AsyncSession):
    """Schema should reject confidence_score in PATCH body (422)."""
    token = await _create_badge2_user(client, "cap4@example.com", db_session)

    resp = await client.patch(
        "/api/v1/capability/document",
        json={"confidence_score": 99, "narrative": "hacked"},
        headers={"Authorization": f"Bearer {token}"},
    )
    # extra="forbid" on the schema means confidence_score causes a 422
    assert resp.status_code == 422
