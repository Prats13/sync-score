import secrets
from typing import Any

import httpx

from app.core.config import settings
from app.core.redis import get_redis

_GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
_GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
_GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

_LINKEDIN_AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization"
_LINKEDIN_TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken"
_LINKEDIN_ME_URL = "https://api.linkedin.com/v2/me"
_LINKEDIN_EMAIL_URL = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))"

_STATE_TTL = 600


async def get_google_authorization_url() -> tuple[str, str]:
    state = secrets.token_urlsafe(32)
    redis = await get_redis()
    await redis.setex(f"oauth_state:{state}", _STATE_TTL, "google")
    params = {
        "client_id": settings.GOOGLE_CLIENT_ID,
        "redirect_uri": settings.GOOGLE_REDIRECT_URI,
        "response_type": "code",
        "scope": "openid email profile",
        "state": state,
    }
    query = "&".join(f"{k}={v}" for k, v in params.items())
    return f"{_GOOGLE_AUTH_URL}?{query}", state


async def exchange_google_code(code: str, state: str) -> dict[str, Any]:
    redis = await get_redis()
    stored = await redis.get(f"oauth_state:{state}")
    if not stored or stored != "google":
        raise ValueError("Invalid or expired OAuth state")
    await redis.delete(f"oauth_state:{state}")

    async with httpx.AsyncClient() as client:
        token_resp = await client.post(
            _GOOGLE_TOKEN_URL,
            data={
                "code": code,
                "client_id": settings.GOOGLE_CLIENT_ID,
                "client_secret": settings.GOOGLE_CLIENT_SECRET,
                "redirect_uri": settings.GOOGLE_REDIRECT_URI,
                "grant_type": "authorization_code",
            },
        )
        token_resp.raise_for_status()
        tokens = token_resp.json()

        userinfo_resp = await client.get(
            _GOOGLE_USERINFO_URL,
            headers={"Authorization": f"Bearer {tokens['access_token']}"},
        )
        userinfo_resp.raise_for_status()
        info = userinfo_resp.json()

    return {"id": info["sub"], "email": info["email"], "name": info.get("name", "")}


async def get_linkedin_authorization_url() -> tuple[str, str]:
    state = secrets.token_urlsafe(32)
    redis = await get_redis()
    await redis.setex(f"oauth_state:{state}", _STATE_TTL, "linkedin")
    params = {
        "response_type": "code",
        "client_id": settings.LINKEDIN_CLIENT_ID,
        "redirect_uri": settings.LINKEDIN_REDIRECT_URI,
        "scope": "r_liteprofile r_emailaddress",
        "state": state,
    }
    query = "&".join(f"{k}={v}" for k, v in params.items())
    return f"{_LINKEDIN_AUTH_URL}?{query}", state


async def exchange_linkedin_code(code: str, state: str) -> dict[str, Any]:
    redis = await get_redis()
    stored = await redis.get(f"oauth_state:{state}")
    if not stored or stored != "linkedin":
        raise ValueError("Invalid or expired OAuth state")
    await redis.delete(f"oauth_state:{state}")

    async with httpx.AsyncClient() as client:
        token_resp = await client.post(
            _LINKEDIN_TOKEN_URL,
            data={
                "grant_type": "authorization_code",
                "code": code,
                "redirect_uri": settings.LINKEDIN_REDIRECT_URI,
                "client_id": settings.LINKEDIN_CLIENT_ID,
                "client_secret": settings.LINKEDIN_CLIENT_SECRET,
            },
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        token_resp.raise_for_status()
        tokens = token_resp.json()
        access_token = tokens["access_token"]

        headers = {"Authorization": f"Bearer {access_token}"}
        me_resp = await client.get(_LINKEDIN_ME_URL, headers=headers)
        me_resp.raise_for_status()
        me = me_resp.json()

        email_resp = await client.get(_LINKEDIN_EMAIL_URL, headers=headers)
        email_resp.raise_for_status()
        email_data = email_resp.json()
        email = email_data["elements"][0]["handle~"]["emailAddress"]

    return {"id": me["id"], "email": email, "name": f"{me.get('localizedFirstName', '')} {me.get('localizedLastName', '')}".strip()}
