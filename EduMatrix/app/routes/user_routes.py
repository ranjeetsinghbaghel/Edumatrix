import os
import re
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from app.face_recognition.utils import save_face_encoding

router = APIRouter()

@router.post("/register")
def register_user(
    name: str = Form(...),
    file: UploadFile = File(...)
):
    # Ensure directory exists
    os.makedirs("images/registered", exist_ok=True)
    
    image_path = f"images/registered/{name}.jpg"

    with open(image_path, "wb") as f:
        f.write(file.file.read())

    try:
        # Extract ID from roll number (e.g. "23cse700" -> "700")
        match = re.search(r"(\d+)$", name)
        save_id = match.group(1) if match else name
        
        save_face_encoding(save_id, image_path)
    except Exception as e:
        if os.path.exists(image_path):
            os.remove(image_path)
        raise HTTPException(status_code=400, detail=str(e))

    return {"message": f"{name} registered successfully"}
