from fastapi import Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app import models
from app.auth import get_current_user as get_user_simple

# Reuse basic extraction logic, but enhance with Audit and RBAC

def get_current_active_user(
    current_user: dict = Depends(get_user_simple),
    db: Session = Depends(get_db)
):
    user = db.query(models.User).filter(models.User.username == current_user["username"]).first()
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    if not user.is_active:
        raise HTTPException(status_code=400, detail="Inactive user")
    return user

def require_permission(permission_name: str):
    def permission_checker(user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
        if user.role == "admin": 
            return user
            
        role_obj = db.query(models.Role).filter(models.Role.name == user.role).first()
        if not role_obj:
             raise HTTPException(status_code=403, detail="Role configuration error")
             
        perms = [p.name for p in role_obj.permissions]
        if permission_name not in perms:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Missing permission: {permission_name}"
            )
        return user
    return permission_checker
