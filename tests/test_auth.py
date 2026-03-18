from unittest.mock import AsyncMock, patch

import pytest
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User


@pytest.mark.asyncio
async def test_register_creates_user(client: AsyncClient, db_session: AsyncSession):
    with patch("app.services.otp_service.get_redis") as mock_redis:
        redis_mock = AsyncMock()
        redis_mock.setex = AsyncMock()
        mock_redis.return_value = redis_mock

        resp = await client.post(
            "/api/v1/auth/register",
            json={"email": "test@example.com", "password": "password123", "entity_type": "individual"},
        )

    assert resp.status_code == 201
    data = resp.json()
    assert data["email"] == "test@example.com"
    assert data["is_active"] is False

    result = await db_session.execute(select(User).where(User.email == "test@example.com"))
    user = result.scalar_one_or_none()
    assert user is not None


@pytest.mark.asyncio
async def test_register_duplicate_email(client: AsyncClient):
    with patch("app.services.otp_service.get_redis") as mock_redis:
        redis_mock = AsyncMock()
        redis_mock.setex = AsyncMock()
        mock_redis.return_value = redis_mock

        await client.post(
            "/api/v1/auth/register",
            json={"email": "dup@example.com", "password": "password123", "entity_type": "individual"},
        )
        resp = await client.post(
            "/api/v1/auth/register",
            json={"email": "dup@example.com", "password": "password123", "entity_type": "individual"},
        )

    assert resp.status_code == 409


@pytest.mark.asyncio
async def test_verify_otp_sets_badge_1(client: AsyncClient):
    email = "otp@example.com"

    with patch("app.services.otp_service.get_redis") as mock_redis:
        redis_mock = AsyncMock()
        redis_mock.setex = AsyncMock()
        mock_redis.return_value = redis_mock

        await client.post(
            "/api/v1/auth/register",
            json={"email": email, "password": "password123", "entity_type": "individual"},
        )

    with patch("app.services.otp_service.get_redis") as mock_redis:
        redis_mock = AsyncMock()
        # Simulate a pre-hashed correct OTP by using passlib
        from passlib.context import CryptContext
        ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")
        otp_plain = "123456"
        redis_mock.get = AsyncMock(return_value=ctx.hash(otp_plain))
        redis_mock.delete = AsyncMock()
        mock_redis.return_value = redis_mock

        resp = await client.post(
            "/api/v1/auth/verify-otp",
            json={"email": email, "otp": otp_plain},
        )

    assert resp.status_code == 200
    data = resp.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_login_returns_tokens(client: AsyncClient):
    email = "login@example.com"

    with patch("app.services.otp_service.get_redis") as mock_redis:
        redis_mock = AsyncMock()
        redis_mock.setex = AsyncMock()
        mock_redis.return_value = redis_mock
        await client.post(
            "/api/v1/auth/register",
            json={"email": email, "password": "password123", "entity_type": "individual"},
        )

    # Activate user via OTP mock
    with patch("app.services.otp_service.get_redis") as mock_redis:
        from passlib.context import CryptContext
        ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")
        redis_mock = AsyncMock()
        redis_mock.get = AsyncMock(return_value=ctx.hash("000000"))
        redis_mock.delete = AsyncMock()
        mock_redis.return_value = redis_mock
        await client.post("/api/v1/auth/verify-otp", json={"email": email, "otp": "000000"})

    resp = await client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "password123"},
    )
    assert resp.status_code == 200
    assert "access_token" in resp.json()


@pytest.mark.asyncio
async def test_protected_route_without_token(client: AsyncClient):
    resp = await client.get("/api/v1/presence/artifacts")
    assert resp.status_code == 403  # HTTPBearer returns 403 when no credentials
