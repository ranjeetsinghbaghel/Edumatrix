from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth_dependencies import get_current_active_user as get_current_user
from app.models import User
from app.models_campus import LostFoundItem
import random

router = APIRouter(prefix="/lost-found", tags=["Aura Found (Lost & Found)"])

class LostItemBase(BaseModel):
    title: str
    description: str
    location_found_or_lost: str
    type: str # "LOST" or "FOUND"
    category: str # "Electronics", "ID/Wallet", "Keys", "Other"
    
class LostItemCreate(LostItemBase):
    pass

class LostItemOut(LostItemBase):
    id: str
    reporter_name: str
    date_reported: str
    status: str # "ACTIVE", "RESOLVED"
    contact_info: str

# Mock Database
INITIAL_ITEMS = [
    {
        "id": "lf-101",
        "title": "Black AirPods Pro",
        "description": "Left earbud missing. Found near the library computers.",
        "location_found_or_lost": "Library 1st Floor",
        "type": "FOUND",
        "category": "Electronics",
        "date_reported": "2024-03-01",
        "status": "ACTIVE",
    },
    {
        "id": "lf-102",
        "title": "Blue Hostel Key",
        "description": "Has a little Spiderman keychain attached.",
        "location_found_or_lost": "Canteen Area",
        "type": "LOST",
        "category": "Keys",
        "date_reported": "2024-03-01",
        "status": "ACTIVE",
    },
    {
        "id": "lf-103",
        "title": "Student ID - 2021CS045",
        "description": "Found lying on the bench.",
        "location_found_or_lost": "Sports Ground",
        "type": "FOUND",
        "category": "ID/Wallet",
        "date_reported": "2024-02-28",
        "status": "RESOLVED",
    }
]

@router.get("/list", response_model=List[LostItemOut])
def get_all_items(db: Session = Depends(get_db)):
    items = db.query(LostFoundItem).order_by(LostFoundItem.created_at.desc()).all()
    if not items:
        # DB is empty, let's look for Admin user to seed items with
        admin = db.query(User).filter(User.role == "ADMIN").first()
        if admin:
            for item_data in INITIAL_ITEMS:
                new_item = LostFoundItem(**item_data, user_id=admin.id)
                db.add(new_item)
            db.commit()
            items = db.query(LostFoundItem).order_by(LostFoundItem.created_at.desc()).all()
            
    out = []
    for item in items:
        user = item.user
        out.append(LostItemOut(
            id=item.id,
            title=item.title,
            description=item.description,
            location_found_or_lost=item.location_found_or_lost,
            type=item.type,
            category=item.category,
            reporter_name=(user.full_name or user.username) if user else "Unknown",
            date_reported=item.date_reported,
            status=item.status,
            contact_info=user.email if (user and user.email) else "Unknown Contact"
        ))
    # Optional sorting could happen here, but we already order_by desc
    return out

@router.post("/report", response_model=LostItemOut)
def report_item(req: LostItemCreate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    new_item = LostFoundItem(
        id=f"lf-{random.randint(1000, 9999)}",
        user_id=current_user.id,
        title=req.title,
        description=req.description,
        location_found_or_lost=req.location_found_or_lost,
        type=req.type,
        category=req.category,
        date_reported=datetime.today().strftime("%Y-%m-%d"),
        status="ACTIVE"
    )
    db.add(new_item)
    db.commit()
    db.refresh(new_item)
    
    return LostItemOut(
        id=new_item.id,
        title=new_item.title,
        description=new_item.description,
        location_found_or_lost=new_item.location_found_or_lost,
        type=new_item.type,
        category=new_item.category,
        reporter_name=current_user.full_name or current_user.username,
        date_reported=new_item.date_reported,
        status=new_item.status,
        contact_info=current_user.email or "Unknown Contact"
    )

@router.put("/resolve/{item_id}", response_model=LostItemOut)
def resolve_item(item_id: str, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    item = db.query(LostFoundItem).filter(LostFoundItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="Item not found")
        
    item.status = "RESOLVED"
    db.commit()
    
    return LostItemOut(
        id=item.id,
        title=item.title,
        description=item.description,
        location_found_or_lost=item.location_found_or_lost,
        type=item.type,
        category=item.category,
        reporter_name=(item.user.full_name or item.user.username) if item.user else "Unknown",
        date_reported=item.date_reported,
        status=item.status,
        contact_info=item.user.email if (item.user and item.user.email) else "Unknown Contact"
    )
