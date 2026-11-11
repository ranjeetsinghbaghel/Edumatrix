from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from typing import List
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth_dependencies import get_current_active_user as get_current_user
from app.models import User
from app.models_campus import CanteenItem, CanteenOrder
import random

router = APIRouter(prefix="/canteen", tags=["Aura Bites (Canteen)"])

# Models
class MenuItem(BaseModel):
    id: str
    name: str
    description: str
    price: float
    category: str
    calories: int
    is_veg: bool
    rating: float
    prep_time_mins: int
    image_emoji: str

class CanteenStatus(BaseModel):
    is_open: bool
    crowd_percentage: int
    wait_time_mins: int
    popular_now: str

class OrderItem(BaseModel):
    menu_id: str
    quantity: int

class OrderRequest(BaseModel):
    items: List[OrderItem]
    pickup_time: str
    notes: str = ""

class OrderResponse(BaseModel):
    order_id: str
    status: str
    total_amount: float
    estimated_ready_time: str
    message: str

# Initial Seed Data defined as dictionaries over the old MenuItem model list so it is easy to seed DB if empty.
INITIAL_MENU_DATA = [
    # Top Picks
    {"id": "m1", "name": "Aura Signature Burger", "description": "Double patty with secret aura sauce", "price": 120.0, "category": "Fast Food", "calories": 550, "is_veg": False, "rating": 4.8, "prep_time_mins": 10, "image_emoji": "🍔"},
    {"id": "m2", "name": "Spicy Paneer Wrap", "description": "Grilled paneer with crunchy veggies", "price": 90.0, "category": "Fast Food", "calories": 320, "is_veg": True, "rating": 4.5, "prep_time_mins": 8, "image_emoji": "🌯"},
    # Healthy Vibes
    {"id": "h1", "name": "Detox Green Salad", "description": "Fresh greens with vinaigrette", "price": 150.0, "category": "Healthy", "calories": 180, "is_veg": True, "rating": 4.2, "prep_time_mins": 5, "image_emoji": "🥗"},
    {"id": "h2", "name": "Protein Oat Bowl", "description": "Oats with berries and honey", "price": 110.0, "category": "Healthy", "calories": 250, "is_veg": True, "rating": 4.6, "prep_time_mins": 7, "image_emoji": "🥣"},
    # Beverages
    {"id": "b1", "name": "Iced Caramel Macchiato", "description": "Chilled espresso with caramel swirl", "price": 140.0, "category": "Beverages", "calories": 220, "is_veg": True, "rating": 4.9, "prep_time_mins": 4, "image_emoji": "🧋"},
    {"id": "b2", "name": "Fresh Watermelon Juice", "description": "No added sugar, pure juice", "price": 60.0, "category": "Beverages", "calories": 90, "is_veg": True, "rating": 4.7, "prep_time_mins": 3, "image_emoji": "🍹"},
    # Desserts
    {"id": "d1", "name": "Molten Choco Lava", "description": "Warm cake with a gooey center", "price": 95.0, "category": "Desserts", "calories": 400, "is_veg": True, "rating": 4.8, "prep_time_mins": 12, "image_emoji": "🍮"}
]

@router.get("/status", response_model=CanteenStatus)
def get_canteen_status():
    """Returns real-time crowd status of the canteen."""
    # Simulating real-time dynamic behavior
    crowd = random.randint(30, 95)
    wait_time = crowd // 5
    
    return CanteenStatus(
        is_open=True,
        crowd_percentage=crowd,
        wait_time_mins=wait_time,
        popular_now="Iced Caramel Macchiato" if crowd > 70 else "Aura Signature Burger"
    )

@router.get("/menu", response_model=List[MenuItem])
def get_menu(db: Session = Depends(get_db)):
    menu_items = db.query(CanteenItem).all()
    if not menu_items:
        # Seed
        for item_data in INITIAL_MENU_DATA:
            new_item = CanteenItem(**item_data)
            db.add(new_item)
        db.commit()
        menu_items = db.query(CanteenItem).all()
    # The models are automatically compliant with Pydantic through from_attributes (we set config or map manually)
    return [MenuItem(**{col.name: getattr(it, col.name) for col in it.__table__.columns}) for it in menu_items]

@router.post("/order", response_model=OrderResponse)
def place_order(
    order: OrderRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    if not order.items:
        raise HTTPException(status_code=400, detail="Order cannot be empty")
        
    total_amount = 0.0
    max_prep_time = 0
    
    for item in order.items:
        menu_item = db.query(CanteenItem).filter(CanteenItem.id == item.menu_id).first()
        if menu_item:
            total_amount += (menu_item.price * item.quantity)
            max_prep_time = max(max_prep_time, menu_item.prep_time_mins)
            
    order_id = f"ORD-{random.randint(1000, 9999)}"
    
    # Save real order
    new_order = CanteenOrder(
        id=order_id,
        user_id=current_user.id,
        total_amount=total_amount,
        status="CONFIRMED"
    )
    db.add(new_order)
    db.commit()
    
    return OrderResponse(
        order_id=order_id,
        status="CONFIRMED",
        total_amount=total_amount,
        estimated_ready_time=f"Ready in {max_prep_time + 2} mins",
        message="Order placed successfully! Please show your digital ID at the counter."
    )
