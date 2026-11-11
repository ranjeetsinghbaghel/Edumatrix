from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app import models
from app.auth_dependencies import require_permission
from app.audit_logger import log_action

router = APIRouter(prefix="/admin", tags=["Admin RBAC"])

@router.patch("/users/{user_id}/disable")
def disable_user(
    user_id: int, 
    admin: models.User = Depends(require_permission("USER_DISABLE")),
    db: Session = Depends(get_db)
):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user: raise HTTPException(404, "User not found")
    
    if user.id == admin.id:
        raise HTTPException(400, "Cannot disable yourself")
        
    user.is_active = False
    db.commit()
    log_action(db, admin.id, "DISABLE_USER", "USER", str(user.id))
    return {"message": f"User {user.username} disabled"}

@router.patch("/users/{user_id}/enable")
def enable_user(
    user_id: int, 
    admin: models.User = Depends(require_permission("USER_UPDATE")),
    db: Session = Depends(get_db)
):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user: raise HTTPException(404, "User not found")
    
    user.is_active = True
    db.commit()
    log_action(db, admin.id, "ENABLE_USER", "USER", str(user.id))
    return {"message": f"User {user.username} enabled"}
