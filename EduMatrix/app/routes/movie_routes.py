from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import MovieEvent, MovieBooking, Student
from app.auth import get_current_user, admin_or_teacher
from pydantic import BaseModel
from typing import List
from ..schemas import MovieEventCreate, MovieEventOut, MovieBookingCreate, MovieBookingOut

router = APIRouter(prefix="/movies", tags=["Movies"])

# --- Student Ends ---

@router.get("/", response_model=List[MovieEventOut])
def get_upcoming_movies(db: Session = Depends(get_db)):
    # Returns all upcoming or now showing movies
    movies = db.query(MovieEvent).filter(MovieEvent.status.in_(["UPCOMING", "NOW_SHOWING"])).all()
    return movies

@router.post("/book", response_model=MovieBookingOut)
def book_movie(
    req: MovieBookingCreate,
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        raise HTTPException(status_code=404, detail="Only students can book tickets.")

    movie = db.query(MovieEvent).filter(MovieEvent.id == req.movie_id).first()
    if not movie:
        raise HTTPException(status_code=404, detail="Movie not found.")

    if movie.available_seats < req.seats_booked:
        raise HTTPException(status_code=400, detail="Not enough seats available.")

    # Check if already booked
    existing_booking = db.query(MovieBooking).filter(
        MovieBooking.movie_id == movie.id, 
        MovieBooking.student_id == student.id,
        MovieBooking.status == "CONFIRMED"
    ).first()
    if existing_booking:
         raise HTTPException(status_code=400, detail="You have already booked this movie.")

    booking = MovieBooking(
        movie_id=movie.id,
        student_id=student.id,
        seats_booked=req.seats_booked
    )
    
    movie.available_seats -= req.seats_booked

    db.add(booking)
    db.commit()
    db.refresh(booking)
    return booking

@router.get("/my-bookings", response_model=List[MovieBookingOut])
def get_my_bookings(
    db: Session = Depends(get_db),
     user=Depends(get_current_user)
):
     student = db.query(Student).filter(Student.user_id == user["id"]).first()
     if not student:
        raise HTTPException(status_code=404, detail="Student not found.")
        
     return db.query(MovieBooking).filter(MovieBooking.student_id == student.id).all()

# --- Admin Ends ---

@router.post("/admin", response_model=MovieEventOut)
def add_movie(
    req: MovieEventCreate,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    movie = MovieEvent(**req.dict())
    movie.available_seats = movie.total_seats
    db.add(movie)
    db.commit()
    db.refresh(movie)
    return movie
