import secrets

from passlib.context import CryptContext

from app.core.config import settings
from app.core.redis import get_redis

_pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def _generate_otp() -> str:
    return str(secrets.randbelow(1_000_000)).zfill(6)


async def generate_and_store_otp(email: str) -> str:
    otp = _generate_otp()
    otp_hash = _pwd_context.hash(otp)
    redis = await get_redis()
    await redis.setex(f"otp:{email}", settings.OTP_TTL_SECONDS, otp_hash)
    return otp


async def verify_otp(email: str, otp: str) -> bool:
    redis = await get_redis()
    key = f"otp:{email}"
    stored_hash = await redis.get(key)
    if not stored_hash:
        return False
    if _pwd_context.verify(otp, stored_hash):
        await redis.delete(key)
        return True
    return False
