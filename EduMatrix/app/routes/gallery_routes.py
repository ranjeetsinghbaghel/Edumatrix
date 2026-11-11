from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import List
import os
import shutil
import time

from app.database import get_db
from app.models_gallery import GalleryFolder, GalleryImage
from app.models import User
from app.schemas_gallery import FolderOut, ImageOut
from app.auth import get_current_user, admin_only

router = APIRouter(prefix="/gallery", tags=["Gallery"])

# --- FOLDERS ---

@router.get("/folders", response_model=List[FolderOut])
def get_folders(db: Session = Depends(get_db)):
    return db.query(GalleryFolder).all()

@router.post("/folders", response_model=FolderOut)
def create_folder(
    name: str = Form(...),
    db: Session = Depends(get_db),
    user=Depends(admin_only) # Only Admin can create folders
):
    existing = db.query(GalleryFolder).filter(GalleryFolder.name == name).first()
    if existing:
        raise HTTPException(status_code=400, detail="Folder already exists")
        
    folder = GalleryFolder(name=name)
    db.add(folder)
    db.commit()
    db.refresh(folder)
    return folder

@router.delete("/folders/{folder_id}")
def delete_folder(
    folder_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    folder = db.query(GalleryFolder).filter(GalleryFolder.id == folder_id).first()
    if not folder:
        raise HTTPException(status_code=404, detail="Folder not found")
        
    # Optional: Delete physical files?
    # For now, just database deletion.
    
    db.delete(folder)
    db.commit()
    return {"message": "Folder deleted"}

# --- IMAGES ---

@router.get("/folders/{folder_id}/images", response_model=List[ImageOut])
def get_images(
    folder_id: int,
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    # If Admin, show ALL (including pending)
    # If Teacher/Student, show ONLY APPROVED
    
    query = db.query(GalleryImage).filter(GalleryImage.folder_id == folder_id)
    print(f"DEBUG: get_images query count (before filter): {query.count()}")
    
    if user["role"] == "admin":
        pass # All
    else:
        query = query.filter(GalleryImage.status == "APPROVED")
        
    print(f"DEBUG: get_images result count (after filter): {query.count()}")
    return query.all()

@router.post("/upload", response_model=ImageOut)
def upload_image(
    folder_id: int = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    # Verify folder exists
    folder = db.query(GalleryFolder).filter(GalleryFolder.id == folder_id).first()
    if not folder:
        raise HTTPException(status_code=404, detail="Folder not found")

    # Status Logic
    # Teacher -> PENDING
    # Admin -> APPROVED
    status = "APPROVED" if user["role"] == "admin" else "PENDING"
    print(f"DEBUG: Uploading image. User role: {user['role']}, Status: {status}")
    
    try:
        # Create Directory: images/gallery/{folder_id}
        base_dir = f"images/gallery/{folder_id}"
        os.makedirs(base_dir, exist_ok=True)
        
        # Unique Filename
        timestamp = int(time.time())
        filename = f"{user['id']}_{timestamp}_{file.filename}"
        file_path = os.path.join(base_dir, filename)
        
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        # Save to DB
        image_url = f"/{base_dir}/{filename}".replace("\\", "/") # Normalize path
        
        gallery_image = GalleryImage(
            folder_id=folder_id,
            image_url=image_url,
            uploaded_by_id=user["id"],
            status=status
        )
        db.add(gallery_image)
        db.commit()
        db.refresh(gallery_image)
        
        return gallery_image
        
    except Exception as e:
        print(f"Gallery Upload Error: {e}")
        raise HTTPException(status_code=500, detail="Failed to upload image")

@router.put("/images/{image_id}/approve")
def approve_image(
    image_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    image = db.query(GalleryImage).filter(GalleryImage.id == image_id).first()
    if not image:
        raise HTTPException(status_code=404, detail="Image not found")
        
    image.status = "APPROVED"
    db.commit()
    return {"message": "Image approved"}

@router.delete("/images/{image_id}")
def delete_image(
    image_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    image = db.query(GalleryImage).filter(GalleryImage.id == image_id).first()
    if not image:
         raise HTTPException(status_code=404, detail="Image not found")
         
    # Optional: Delete from disk
    try:
        local_path = image.image_url.lstrip("/")
        if os.path.exists(local_path):
            os.remove(local_path)
    except:
        pass
        
    db.delete(image)
    db.commit()
    return {"message": "Image deleted"}
