from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, DateTime, Date
from sqlalchemy.orm import relationship
from app.database import Base
from datetime import datetime

class CanteenItem(Base):
    __tablename__ = "canteen_items"
    
    id = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False)
    description = Column(String)
    price = Column(Float, nullable=False)
    category = Column(String)
    calories = Column(Integer)
    is_veg = Column(Boolean, default=True)
    rating = Column(Float, default=0.0)
    prep_time_mins = Column(Integer, default=10)
    image_emoji = Column(String)

class CanteenOrder(Base):
    __tablename__ = "canteen_orders"
    
    id = Column(String, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    total_amount = Column(Float, nullable=False)
    status = Column(String, default="PENDING") # PENDING, PREPARING, READY, COMPLETED
    created_at = Column(DateTime, default=datetime.utcnow)
    
    user = relationship("User")

class FacilitySpace(Base):
    __tablename__ = "facility_spaces"
    
    id = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False)
    type = Column(String, nullable=False)
    capacity = Column(Integer)
    is_available = Column(Boolean, default=True)
    next_available_time = Column(String, nullable=True)
    icon_emoji = Column(String)
    location = Column(String)

class SpaceBooking(Base):
    __tablename__ = "space_bookings"
    
    id = Column(String, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    space_id = Column(String, ForeignKey("facility_spaces.id"))
    date = Column(String, nullable=False)
    time_slot = Column(String, nullable=False)
    purpose = Column(String)
    status = Column(String, default="CONFIRMED")
    created_at = Column(DateTime, default=datetime.utcnow)
    
    user = relationship("User")
    space = relationship("FacilitySpace")

class LostFoundItem(Base):
    __tablename__ = "lost_found_items"
    
    id = Column(String, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    title = Column(String, nullable=False)
    description = Column(String)
    location_found_or_lost = Column(String)
    type = Column(String) # LOST, FOUND
    category = Column(String)
    date_reported = Column(String)
    status = Column(String, default="ACTIVE") # ACTIVE, RESOLVED
    created_at = Column(DateTime, default=datetime.utcnow)
    
    user = relationship("User")
