from datetime import datetime
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Boolean
from sqlalchemy.orm import relationship
from app.database import Base

class GalleryFolder(Base):
    __tablename__ = "gallery_folders"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    images = relationship("GalleryImage", back_populates="folder", cascade="all, delete-orphan")

class GalleryImage(Base):
    __tablename__ = "gallery_images"
    
    id = Column(Integer, primary_key=True, index=True)
    folder_id = Column(Integer, ForeignKey("gallery_folders.id"))
    image_url = Column(String, nullable=False)
    uploaded_by_id = Column(Integer, ForeignKey("users.id"))
    status = Column(String, default="PENDING") # PENDING, APPROVED
    created_at = Column(DateTime, default=datetime.utcnow)
    
    folder = relationship("GalleryFolder", back_populates="images")
    uploader = relationship("User")
