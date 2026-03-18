from fastapi import APIRouter, Depends

from app.deps import require_badge_3
from app.models.user import User

router = APIRouter(prefix="/execution", tags=["execution"])


@router.get("/challenge")
async def get_challenge(current_user: User = Depends(require_badge_3)) -> dict:
    return {
        "status": "not_implemented",
        "message": "Execution challenge system coming in Badge 4 phase",
        "user_id": str(current_user.id),
    }


@router.post("/submit")
async def submit_execution(current_user: User = Depends(require_badge_3)) -> dict:
    return {"status": "not_implemented"}
