from datetime import datetime, timedelta
from jose import jwt, JWTError
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
import bcrypt

SECRET_KEY = "auraface-secret-key-change-this"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")

def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

def verify_password(plain_password: str, stored_password: str) -> bool:
    try:
        return bcrypt.checkpw(plain_password.encode('utf-8'), stored_password.encode('utf-8'))
    except Exception:
        return False

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

def get_current_user(token: str = Depends(oauth2_scheme)):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username = payload.get("sub")
        role = payload.get("role")

        if username is None:
            raise HTTPException(status_code=401, detail="Invalid token")

        return {"username": username, "role": role, "id": payload.get("id")}

    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token expired or invalid"
        )
def admin_or_teacher(user=Depends(get_current_user)):
    if user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user
   
def admin_only(user=Depends(get_current_user)):
    if user["role"] != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access only"
        )
    return user

def teacher_only(user=Depends(get_current_user)):
    if user["role"] != "teacher":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Teacher access only"
        )
    return user

def student_only(user=Depends(get_current_user)):
    if user["role"] != "student":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Student access only"
        )
    return user

def admin_or_student(user=Depends(get_current_user)):
    if user["role"] not in ["admin", "student"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user

def teacher_or_student(user=Depends(get_current_user)):
    if user["role"] not in ["teacher", "student"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user

def admin_teacher_student(user=Depends(get_current_user)):
    if user["role"] not in ["admin", "teacher", "student"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user

def admin_teacher(user=Depends(get_current_user)):
    if user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user

def teacher_student(user=Depends(get_current_user)):
    if user["role"] not in ["teacher", "student"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user

def admin_student(user=Depends(get_current_user)):
    if user["role"] not in ["admin", "student"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user

def any_role(user=Depends(get_current_user)):
    if user["role"] not in ["admin", "teacher", "student"]:
        raise HTTPException(status_code=403, detail="Access denied")
    return user

