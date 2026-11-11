from pydantic import BaseModel
from datetime import datetime
from typing import List, Optional

class FolderCreate(BaseModel):
    name: str

class FolderOut(BaseModel):
    id: int
    name: str
    created_at: datetime
    
    class Config:
        from_attributes = True

class ImageUpload(BaseModel):
    folder_id: int
    
class ImageOut(BaseModel):
    id: int
    folder_id: int
    image_url: str
    uploaded_by_id: int
    status: str
    created_at: datetime
    
    class Config:
        from_attributes = True
