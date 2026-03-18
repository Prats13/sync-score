from typing import AsyncGenerator

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import AsyncSessionLocal
from app.core.security import decode_token
from app.models.user import User

bearer_scheme = HTTPBearer()


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    token = credentials.credentials
    try:
        payload = decode_token(token)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired token")

    if payload.get("type") != "access":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token type")

    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token payload")

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Account not activated")

    return user


async def require_badge_1(current_user: User = Depends(get_current_user)) -> User:
    if not current_user.badge_1:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Badge 1 (Identity Verified) required",
        )
    return current_user


async def require_badge_2(user: User = Depends(require_badge_1)) -> User:
    if not user.badge_2:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Badge 2 (Presence Verified) required",
        )
    return user


async def require_badge_3(user: User = Depends(require_badge_2)) -> User:
    if not user.badge_3:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Badge 3 (Capability Verified) required",
        )
    return user
