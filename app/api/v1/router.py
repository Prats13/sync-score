from fastapi import APIRouter

from app.api.v1 import auth, capability, execution, presence

router = APIRouter(prefix="/api/v1")

router.include_router(auth.router)
router.include_router(presence.router)
router.include_router(capability.router)
router.include_router(execution.router)
