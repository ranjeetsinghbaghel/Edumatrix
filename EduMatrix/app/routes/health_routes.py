from fastapi import APIRouter

router = APIRouter(tags=["Health"])

@router.get("/ping")
def ping():
    return {
        "status": "ok",
        "message": "EduMatrix backend alive"
    }
