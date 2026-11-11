# EduMatrix Main Application - Reload Triggered by renaming serviceAccountKey.json
from dotenv import load_dotenv
load_dotenv() # Load environment variables from .env file

from fastapi import FastAPI, Request
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware

from app.database import engine, Base
from app.routes.attendance_routes import router as attendance_router
from app.routes.admin_routes import router as admin_router
from app.routes.auth_routes import router as auth_router
from app.routes.teacher_routes import router as teacher_router
from app.routes.student_routes import router as student_router
from app.routes.health_routes import router as health_router
from app.face_recognition.recognize import load_known_faces
from app.routes import (
    admin_leave, admin_students, admin_teachers, admin_subjects, admin_attendance,admin_dashboard_charts,
    teacher_attendance, student_leave, teacher_leave, admin_timetable
)


from app.routes.notification_routes import router as notification_router

from app import models_quiz # Ensure tables are created
from app import models_campus # Ensure campus tables are created
from app import models_advanced # Ensure advanced features tables are created

# Create DB tables
Base.metadata.create_all(bind=engine)

import asyncio
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Load faces
    load_known_faces()
    try:
        yield
    except asyncio.CancelledError:
        pass
    except Exception as e:
        print(f"Lifespan error: {e}")
    finally:
        pass

app = FastAPI(title="EduMatrix", lifespan=lifespan)

from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi import Request

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    print(f'422 Error: {exc.errors()}', flush=True)
    try:
        body = await request.body()
        print(f'Body: {body}', flush=True)
    except:
        pass
    return JSONResponse(status_code=422, content={'detail': exc.errors()})

# Routers
app.include_router(auth_router)
app.include_router(notification_router)
app.include_router(admin_router)
app.include_router(teacher_router)
app.include_router(student_router)
app.include_router(health_router)

app.include_router(admin_students.router)
app.include_router(admin_teachers.router)
app.include_router(admin_subjects.router)
app.include_router(admin_attendance.router)
app.include_router(teacher_attendance.router)
app.include_router(student_leave.router)
app.include_router(teacher_leave.router)
app.include_router(admin_leave.router)
app.include_router(admin_dashboard_charts.router)
app.include_router(admin_timetable.router)

from app.routes import chat_routes, insights_routes
app.include_router(chat_routes.router)
app.include_router(insights_routes.router)

from app.routes import academic_routes
app.include_router(academic_routes.router)


from app.routes import gallery_routes
app.include_router(gallery_routes.router)

from app.routes import placement_routes, quiz_routes, pulse_routes, quest_routes, canteen_routes, spaces_routes, lost_found_routes, movie_routes, sports_routes, fee_routes, erp_routes
app.include_router(placement_routes.router)
app.include_router(quiz_routes.router)
app.include_router(pulse_routes.router)
app.include_router(quest_routes.router)
app.include_router(canteen_routes.router)
app.include_router(spaces_routes.router)
app.include_router(lost_found_routes.router)
app.include_router(movie_routes.router)
app.include_router(sports_routes.router)
app.include_router(fee_routes.router)
app.include_router(erp_routes.router)


# Static files
app.mount("/static", StaticFiles(directory="app/static"), name="static")
app.mount("/images", StaticFiles(directory="images"), name="images")

# Templates
templates = Jinja2Templates(directory="app/templates")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ✅ SINGLE dashboard route
@app.get("/", response_class=HTMLResponse)
def dashboard(request: Request):
    return templates.TemplateResponse(
        "dashboard.html",
        {"request": request}
    )

from fastapi.responses import FileResponse
import os

@app.get("/ping")
def ping():
    return {"status": "EduMatrix backend running"}

@app.get("/download-apk")
def download_apk():
    # Try root folder or local folder
    for path in ["../app-debug.apk", "app-debug.apk", "../../app-debug.apk"]:
        if os.path.exists(path):
            return FileResponse(path, media_type="application/vnd.android.package-archive", filename="EduMatrix.apk")
    return JSONResponse(status_code=404, content={"message": "APK file not found"})


app.include_router(attendance_router)

