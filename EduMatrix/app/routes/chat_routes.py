from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect, Query, UploadFile, File, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app import models, schemas
from app.auth_dependencies import get_current_active_user
from app.auth import get_current_user, SECRET_KEY, ALGORITHM
from app.utils.websocket_manager import manager
from jose import jwt, JWTError
from datetime import datetime, date
import shutil
import os
import json
import asyncio
from app import firebase_utils
# Initialize Free AI (G4F) which doesn't require API keys!
try:
    import g4f
    g4f_available = True
except ImportError:
    g4f_available = False

router = APIRouter(prefix="/chat", tags=["Real-time Chat"])

# --- Helper for WS Auth ---
async def get_user_from_token(token: str, db: Session):
    try:
        # Reusing the decoding logic from auth.py manually 
        # since verify_token dependency is not async-friendly for direct call without Depends
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username = payload.get("sub")
        if username is None:
            return None
        user = db.query(models.User).filter(models.User.username == username).first()
        return user
    except JWTError:
        return None

# --- Existing AI Chat ---
@router.post("/ask", response_model=schemas.ChatResponse)
def ask_aura(
    query: schemas.ChatRequest,
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    q = query.message.lower()
    student = db.query(models.Student).filter(models.Student.user_id == user.id).first()
    
    if not student:
        return {"reply": "I can only help students with attendance queries right now."}

    from sqlalchemy import func
    
    def get_attendance_stats():
        dept = student.department.strip() if student.department else ""
        sec = student.section.strip() if student.section else ""
        
        valid_subjects = db.query(models.Subject).filter(
            func.lower(models.Subject.department) == func.lower(dept),
            models.Subject.year == student.year,
            func.lower(models.Subject.section) == func.lower(sec)
        ).all()
        valid_subject_ids = [s.id for s in valid_subjects]

        total_classes = db.query(
            models.Attendance.subject, models.Attendance.date, models.Attendance.period
        ).join(models.Student, models.Attendance.student_id == models.Student.id).filter(
            func.lower(models.Student.department) == func.lower(dept),
            models.Student.year == student.year,
            func.lower(models.Student.section) == func.lower(sec),
            models.Attendance.subject.in_(valid_subject_ids)
        ).distinct().count()
        
        present_classes = db.query(
            models.Attendance.subject, models.Attendance.date, models.Attendance.period
        ).filter(
            models.Attendance.student_id == student.id, 
            models.Attendance.subject.in_(valid_subject_ids),
            func.lower(models.Attendance.status).in_(["present", "late"])
        ).distinct().count()
        
        return total_classes, present_classes

    import re
    target_match = re.search(r'(\d+)\s*%', q)
    is_target_query = ("how many" in q or "need" in q or "make" in q or "reach" in q) and ("attend" in q or "class" in q or "attendance" in q)
    
    # 1. Goal/Target Attendance (e.g., "to make 80%", "how many classes to attend")
    if is_target_query and "miss" not in q and "skip" not in q and "bunk" not in q:
        target_pct = float(target_match.group(1)) if target_match else 75.0
        
        if target_pct <= 0 or target_pct > 100:
            return {"reply": "Please specify a valid target percentage between 1 and 100."}
            
        total, present = get_attendance_stats()
        if total == 0:
            return {"reply": "You haven't attended any classes yet."}
            
        current_pct = (present / total) * 100
        if current_pct >= target_pct:
            return {"reply": f"You already have {current_pct:.1f}%, which is above the target of {target_pct:.0f}%!"}
            
        if target_pct == 100:
             return {"reply": "Since you have already missed some classes, it's impossible to reach exactly 100% again, but you can get very close if you stop missing classes!"}
            
        target_ratio = target_pct / 100.0
        required = (target_ratio * total - present) / (1 - target_ratio)
        import math
        req_int = math.ceil(required)
        return {
            "reply": f"Your current attendance is {current_pct:.1f}%. To safely reach {target_pct:.0f}%, you must attend the next {req_int} consecutive classes!",
            "action": "NAVIGATE_INSIGHTS"
        }

    # 2. Miss classes / Shortage (Standard Risk Calculation)
    elif "miss" in q or "shortage" in q or "skip" in q or "bunk" in q:
        total, present = get_attendance_stats()
        
        if total == 0:
            return {"reply": "You haven't attended any classes yet, so it's hard to predict risk."}
            
        current_pct = (present / total) * 100
        target_pct = float(target_match.group(1)) if target_match else 75.0
        if target_pct <= 0 or target_pct > 100:
             target_pct = 75.0
             
        target_ratio = target_pct / 100.0
        
        if current_pct < target_pct:
             if target_pct == 100:
                 return {"reply": "You cannot afford to miss any classes because you want to maintain 100%."}
             required = (target_ratio * total - present) / (1 - target_ratio)
             import math
             req_int = math.ceil(required)
             if req_int < 0: req_int = 0
             return {
                 "reply": f"Your attendance ({current_pct:.1f}%) is below {target_pct:.0f}%. You need to attend the next {req_int} classes to catch up!",
                 "action": "NAVIGATE_INSIGHTS"
             }
        else:
             if target_pct == 100:
                  return {"reply": "If your target is 100%, you cannot afford to miss any classes at all."}
             missable = int((present / target_ratio) - total)
             return {
                 "reply": f"You are safe! Your attendance is {current_pct:.1f}%. You can afford to miss about {missable} classes before dropping below {target_pct:.0f}%.",
                 "action": "NAVIGATE_INSIGHTS"
             }

    # 3. Simple Attendance Status Check
    elif "attendance" in q or "percentage" in q or "status" in q:
        total, present = get_attendance_stats()
        
        if total > 0:
            pct = (present / total) * 100
            return {
                "reply": f"Your overall attendance is {pct:.1f}%. You have attended {present} out of {total} classes.", 
                "action": "NAVIGATE_ATTENDANCE"
            }
        else:
             return {"reply": "No attendance records found yet."}

    elif "next" in q or "class" in q:
        now = datetime.now()
        today = date.today()
        
        next_session = db.query(models.ClassSession).filter(
            models.ClassSession.department == student.department,
            models.ClassSession.year == student.year,
            models.ClassSession.section == student.section,
            models.ClassSession.date == today,
            models.ClassSession.start_time > now
        ).order_by(models.ClassSession.start_time).first()
        
        if next_session:
             return {"reply": f"Your next class is {next_session.subject_id} starting at {next_session.start_time.strftime('%I:%M %p')}."}
        else:
             return {"reply": "You have no more classes scheduled for today!"}
             
    elif "name" in q or "who am i" in q or "whoami" in q:
        name = user.full_name or user.username
        dept = student.department or "Unknown Department"
        return {"reply": f"You are {name}! A student in the {dept} department."}

    # 4. Academic Help: Schedule / Timetable
    elif "schedule" in q or "timetable" in q or "classes today" in q:
        from app.models import ClassSchedule
        today = date.today()
        day_name = today.strftime("%A")
        
        routines = db.query(ClassSchedule).filter(
            ClassSchedule.department == student.department,
            ClassSchedule.year == student.year,
            ClassSchedule.section == student.section,
            ClassSchedule.day_of_week == day_name
        ).order_by(ClassSchedule.time_slot).all()
        
        if routines:
             routine_text = f"Here is your schedule for {day_name}:\n"
             for r in routines:
                 routine_text += f"- {r.time_slot}: {r.subject} in {r.room or 'Unknown Room'}\n"
             return {"reply": routine_text, "action": "NAVIGATE_TIMETABLE"}
        else:
             return {"reply": "You don't have any classes scheduled for today!"}

    # 5. Academic Help: Exams & Syllabus
    elif "exam" in q or "syllabus" in q or "test" in q:
        return {
            "reply": "Your next internal exams are scheduled tentatively for the end of the month. The syllabus will cover the first 3 modules of your core subjects. Keep studying!", 
            "action": "NAVIGATE_ACADEMIC_CALENDAR"
        }

    # 6. Academic Help: AI Study Planner
    elif "study" in q or "plan" in q or "planner" in q:
        return {
            "reply": "Based on your timetable and recent attendance, you should allocate at least 1.5 hours today for your core subjects. I can generate a personalized AI Study Schedule for you.",
            "action": "NAVIGATE_STUDY_PLANNER"
        }

    # Try G4F if configured, else fallback
    if g4f_available:
        try:
            prompt = f"You are Aura AI, a highly intelligent, empathetic, and friendly university assistant. You are currently talking to {user.full_name}, a university student. Answer this clearly, casually, and concisely: {q}"
            
            response = g4f.ChatCompletion.create(
                model="gpt-4", # Works flawlessly
                messages=[{"role": "user", "content": prompt}]
            )
            
            # G4F sometimes returns a raw string or occasionally throws if a specific free provider is down
            if response and isinstance(response, str):
                return {"reply": response}
            else:
                return {"reply": "I'm thinking really hard right now but my neural network is a bit slow. Let's talk about your attendance or schedules instead!"}
        except Exception as e:
            return {"reply": "I'm having trouble connecting to my neural network right now. Try asking me about your attendance or schedule!"}

    return {
        "reply": "I'm still learning! You can try asking me about your 'attendance status', 'can I miss a class', or 'next class'."
    }

# --- New Real-time Group Chat Routes ---

@router.get("/groups", response_model=list[schemas.ChatGroupOut])
def get_my_groups(
    user: dict = Depends(get_current_user), # generic user dict
    db: Session = Depends(get_db)
):
    current_user_id = user["id"]
    role = user["role"]
    
    groups = []
    seen_groups = set()
    
    def add_group(gid, name, dept, year, sec, profile_img=None):
        if gid not in seen_groups:
            # Query metadata
            meta = db.query(models.ChatGroupMetadata).filter(models.ChatGroupMetadata.group_id == gid).first()
            if meta:
                if meta.name:
                    name = meta.name
                if meta.profile_image:
                    profile_img = meta.profile_image

            last_msg = db.query(models.ChatMessage).filter(models.ChatMessage.group_id == gid).order_by(models.ChatMessage.timestamp.desc()).first()
            unread_count = db.query(models.ChatMessage).filter(
                models.ChatMessage.group_id == gid,
                models.ChatMessage.sender_id != current_user_id,
                models.ChatMessage.is_deleted == False,
                ~models.ChatMessage.reads.any(models.MessageReadStatus.user_id == current_user_id)
            ).count()

            grp = schemas.ChatGroupOut(
                group_id=gid,
                name=name,
                department=dept, year=year, section=sec,
                last_message=last_msg, unread_count=unread_count,
                profile_image=profile_img
            )
            groups.append(grp)
            seen_groups.add(gid)

    if role == "student":
        student = db.query(models.Student).filter(models.Student.user_id == current_user_id).first()
        if student:
            dept = str(student.department).strip().upper() if student.department else ""
            year = student.year
            sec = str(student.section).strip().upper() if student.section else ""
            base_prefix = f"{dept}-{year}-{sec}"
            
            # Official Class Group
            add_group(base_prefix, f"{dept} {year}-{sec} (Class group)", dept, year, sec)
            
            # Any Custom Teacher Groups
            msgs = db.query(models.ChatMessage.group_id).filter(
                models.ChatMessage.group_id.like(f"{base_prefix}-T%")
            ).distinct().all()
            for (gid,) in msgs:
                parts = gid.split("-")
                if len(parts) >= 4 and parts[3].startswith("T"):
                    t_id_str = parts[3][1:]
                    if t_id_str.isdigit():
                        t_id = int(t_id_str)
                        teacher = db.query(models.User).filter(models.User.id == t_id).first()
                        if teacher:
                            subject_name = "-".join(parts[4:]).replace("_", " ") if len(parts) > 4 else (teacher.full_name or teacher.username)
                            add_group(gid, f"{dept} {year}-{sec} ({subject_name})", dept, year, sec, teacher.profile_image)
                        else:
                            subject_name = "-".join(parts[4:]).replace("_", " ") if len(parts) > 4 else "Teacher"
                            add_group(gid, f"{dept} {year}-{sec} ({subject_name})", dept, year, sec)
            
            from sqlalchemy import func
            # Known Subjects for this student
            subjects = db.query(models.Subject).filter(
                func.lower(models.Subject.department) == func.lower(student.department),
                models.Subject.year == student.year,
                func.lower(models.Subject.section) == func.lower(student.section)
            ).all()
            
            for sub in subjects:
                gid = f"{dept}-{year}-{sec}-{str(sub.id)}"
                add_group(gid, f"{dept} {year}-{sec} ({sub.name})", dept, year, sec)

    elif role == "teacher":
         # 1. Class Teacher Groups
         meta = db.query(models.SectionMetadata).filter(models.SectionMetadata.class_teacher_id == current_user_id).all()
         for m in meta:
             dept = str(m.department).strip().upper()
             year = m.year
             sec = str(m.section).strip().upper()
             group_id = f"{dept}-{year}-{sec}"
             add_group(group_id, f"{dept} {year}-{sec} (Class group)", dept, year, sec)
             
         # 2. Subject Teacher Groups
         subjects = db.query(models.Subject).filter(models.Subject.teacher_id == current_user_id).all()
         for sub in subjects:
             if sub.department and sub.year and sub.section:
                 dept = str(sub.department).strip().upper()
                 year = sub.year
                 sec = str(sub.section).strip().upper()
                 gid = f"{dept}-{year}-{sec}-{str(sub.id)}"
                 add_group(gid, f"{dept} {year}-{sec} ({sub.name})", dept, year, sec)
                 
         # 3. Custom Groups teacher sent messages to
         teacher_msgs = db.query(models.ChatMessage.group_id).filter(
             models.ChatMessage.sender_id == current_user_id,
             ~models.ChatMessage.group_id.like("DM_%")
         ).distinct().all()
         for (gid,) in teacher_msgs:
             if gid not in seen_groups:
                 parts = gid.split("-")
                 if len(parts) > 4 and parts[3].startswith("T"):
                     custom_subj = "-".join(parts[4:]).replace("_", " ")
                     name = f"{parts[0]} {parts[1]}-{parts[2]} ({custom_subj})"
                 elif len(parts) == 3:
                     name = f"{parts[0]} {parts[1]}-{parts[2]} (Class group)"
                 else:
                     name = gid if len(parts) < 3 else f"{parts[0]} {parts[1]}-{parts[2]} ({'-'.join(parts[3:])})" if len(parts) > 3 else f"{parts[0]} {parts[1]}-{parts[2]} (Custom)"
                 add_group(gid, name, parts[0] if len(parts)>0 else None, parts[1] if len(parts)>1 else None, parts[2] if len(parts)>2 else None)

    # 4. Add Direct Messages
    dm_messages = db.query(models.ChatMessage.group_id).filter(
        models.ChatMessage.group_id.like("DM_%")
    ).distinct().all()
    
    for (gid,) in dm_messages:
        parts = gid.split("_")
        if len(parts) == 3:
            if str(current_user_id) == parts[1] or str(current_user_id) == parts[2]:
                other_user_id = str(parts[2]) if str(current_user_id) == parts[1] else str(parts[1])
                other_user = db.query(models.User).filter(models.User.id == int(other_user_id)).first()
                if other_user:
                    add_group(gid, other_user.full_name or other_user.username, None, None, None, other_user.profile_image)

    return groups

@router.get("/contacts")
def get_chat_contacts(user: dict = Depends(get_current_user), db: Session = Depends(get_db)):
    role = user["role"]
    contacts = []
    
    # Students can message Teachers
    if role == "student":
        teachers = db.query(models.User).filter(models.User.role == "teacher", models.User.is_active == True).all()
        for t in teachers:
            contacts.append({"id": t.id, "name": t.full_name or t.username, "role": "teacher"})
            
    # Teachers can message Students
    elif role == "teacher":
        students = db.query(models.User).filter(models.User.role == "student", models.User.is_active == True).all()
        for s in students:
            contacts.append({"id": s.id, "name": s.full_name or s.username, "role": "student"})
            
    return contacts

@router.get("/history/{group_id}", response_model=list[schemas.ChatMessageOut])
def get_chat_history(
    group_id: str,
    skip: int = 0,
    limit: int = 50,
    user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    msgs = db.query(models.ChatMessage).filter(
        models.ChatMessage.group_id == group_id
    ).order_by(models.ChatMessage.timestamp.desc()).offset(skip).limit(limit).all()
    
    # Mark as read for this user
    unread_msgs = [m for m in msgs if m.sender_id != user["id"] and not any(r.user_id == user["id"] for r in m.reads)]
    if unread_msgs:
        for m in unread_msgs:
            read_status = models.MessageReadStatus(
                message_id=m.id,
                user_id=user["id"],
                status="READ"
            )
            db.add(read_status)
        try:
            db.commit()
        except:
            db.rollback()

    # We return them in DESC order (newest first). Client should reverse.
    return msgs

@router.post("/upload")
async def upload_attachment(
    file: UploadFile = File(...),
    user: dict = Depends(get_current_user)
):
    safe_filename = f"{datetime.now().timestamp()}_{file.filename}"
    file_location = f"images/chat/{safe_filename}"
    os.makedirs(os.path.dirname(file_location), exist_ok=True)
    
    with open(file_location, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    return {"url": f"/images/chat/{safe_filename}", "filename": file.filename, "size": "Unknown"}

@router.websocket("/ws")
async def global_websocket_endpoint(
    websocket: WebSocket,
    token: str = Query(...),
    db: Session = Depends(get_db)
):
    user = await get_user_from_token(token, db)
    if not user:
        await websocket.close(code=4003, reason="Unauthorized")
        return

    await websocket.accept()
    
    try:
        while True:
            data = await websocket.receive_json()
            action = data.get("action")
            
            if action == "subscribe":
                groups = data.get("groups", [])
                for group_id in groups:
                    await manager.connect(websocket, group_id, str(user.id), accept=False)
                    
            elif action == "send":
                group_id = data.get("group_id")
                if not group_id:
                    continue
                    
                # Save to DB
                new_msg = models.ChatMessage(
                    sender_id=user.id,
                    group_id=group_id,
                    content=data.get("content"),
                    msg_type=data.get("msg_type", "TEXT"),
                    attachment_url=data.get("attachment_url"),
                    filename=data.get("filename"),
                    reply_to_id=data.get("reply_to_id"),
                    timestamp=datetime.utcnow()
                )
                db.add(new_msg)
                db.commit()
                db.refresh(new_msg)
                
                # Broadcast
                msg_data = {
                    "id": new_msg.id,
                    "sender_id": user.id,
                    "sender_name": user.full_name or user.username,
                    "sender_profile_image": user.profile_image,
                    "group_id": group_id,
                    "content": new_msg.content,
                    "msg_type": new_msg.msg_type,
                    "attachment_url": new_msg.attachment_url,
                    "filename": new_msg.filename,
                    "timestamp": new_msg.timestamp.isoformat(),
                    "reply_to_id": new_msg.reply_to_id,
                    "status": "DELIVERED"
                }
                
                await manager.broadcast_to_group(msg_data, group_id)
                
                # Push FCM Notification
                try:
                    fcm_tokens = []
                    if group_id.startswith("DM_"):
                        parts = group_id.split("_")
                        if len(parts) >= 3:
                            u1, u2 = parts[1], parts[2]
                            target_id = int(u2) if str(user.id) == u1 else int(u1)
                            t_user = db.query(models.User).filter(
                                models.User.id == target_id, 
                                models.User.fcm_token.isnot(None),
                                models.User.fcm_token != ""
                            ).first()
                            if t_user:
                                fcm_tokens.append(t_user.fcm_token)
                    else:
                        parts = group_id.split("-")
                        if len(parts) >= 3:
                            dept, year_str, sec = parts[0], parts[1], parts[2]
                            if year_str.isdigit():
                                target_students = db.query(models.User.fcm_token).join(models.Student).filter(
                                    models.User.id != user.id,
                                    models.User.fcm_token.isnot(None),
                                    models.User.fcm_token != "",
                                    models.Student.department == dept,
                                    models.Student.year == int(year_str),
                                    models.Student.section == sec
                                ).all()
                                fcm_tokens.extend([t[0] for t in target_students])
                    
                    if fcm_tokens:
                        if user.fcm_token:
                            fcm_tokens = [tok for tok in fcm_tokens if tok != user.fcm_token]
                            
                    if fcm_tokens:
                        sender_name = user.full_name or user.username
                        title_text = f"New message from {sender_name}" 
                        if not group_id.startswith("DM_"):
                            fetched_name = group_id
                            meta = db.query(models.ChatGroupMetadata).filter(models.ChatGroupMetadata.group_id == group_id).first()
                            if meta and meta.name:
                                fetched_name = meta.name
                            else:
                                g_parts = group_id.split("-")
                                if len(g_parts) == 3:
                                    fetched_name = f"{g_parts[0]} {g_parts[1]}-{g_parts[2]} (Class group)"
                                elif len(g_parts) >= 4:
                                    if g_parts[3].startswith("T"):
                                        fetched_name = f"{g_parts[0]} {g_parts[1]}-{g_parts[2]} (Subject)"
                                    else:
                                        sub = db.query(models.Subject).filter(models.Subject.id == g_parts[3]).first()
                                        if sub:
                                            fetched_name = f"{g_parts[0]} {g_parts[1]}-{g_parts[2]} ({sub.name})"
                                            
                            title_text = f"New message in {fetched_name} from {sender_name}"
                        
                        body_text = new_msg.content if new_msg.content else f"Sent an attachment"
                        
                        asyncio.create_task(
                            asyncio.to_thread(
                                firebase_utils.send_multicast_notification,
                                fcm_tokens,
                                title_text,
                                body_text,
                                {"type": "CHAT", "group_id": group_id},
                                new_msg.attachment_url,
                                False
                            )
                        )
                except Exception as ex:
                    print(f"Failed to push FCM: {ex}")
            
    except Exception as e:
        print(f"WebSocket Error: {e}")
        manager.remove_socket_from_all(websocket, str(user.id))

@router.delete("/messages/{message_id}")
async def delete_message(
    message_id: int,
    user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Fetch message
    msg = db.query(models.ChatMessage).filter(models.ChatMessage.id == message_id).first()
    if not msg:
        raise HTTPException(status_code=404, detail="Message not found")
        
    # Check permissions (Sender only)
    if msg.sender_id != user["id"]:
        raise HTTPException(status_code=403, detail="Not authorized to delete this message")
        
    # Soft Delete
    msg.is_deleted = True
    msg.content = "This message was deleted"
    msg.attachment_url = None # Remove attachment
    msg.filename = None
    db.commit()
    
    # Broadcast Update
    msg_data = {
        "id": msg.id,
        "sender_id": msg.sender_id,
        "sender_name": msg.sender_name, # helper property
        "sender_profile_image": msg.sender_profile_image,
        "group_id": msg.group_id,
        "content": msg.content,
        "msg_type": "TEXT", # Revert to text
        "attachment_url": None,
        "filename": None,
        "timestamp": msg.timestamp.isoformat(),
        "is_deleted": True,
        "status": "DELIVERED"
    }
    await manager.broadcast_to_group(msg_data, msg.group_id)
    
    return {"message": "Message deleted"}

class EditMessageRequest(schemas.BaseModel):
    content: str

@router.put("/messages/{message_id}")
async def edit_message(
    message_id: int,
    req: EditMessageRequest,
    user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    msg = db.query(models.ChatMessage).filter(models.ChatMessage.id == message_id).first()
    if not msg:
        raise HTTPException(status_code=404, detail="Message not found")
        
    if msg.sender_id != user["id"]:
        raise HTTPException(status_code=403, detail="Not authorized to edit this message")
        
    if msg.is_deleted:
         raise HTTPException(status_code=400, detail="Cannot edit deleted message")

    msg.content = req.content
    # Append (edited) ?? Maybe UI handles it
    db.commit()
    
    msg_data = {
        "id": msg.id,
        "sender_id": msg.sender_id,
        "sender_name": msg.sender_name,
        "sender_profile_image": msg.sender_profile_image,
        "group_id": msg.group_id,
        "content": msg.content,
        "msg_type": msg.msg_type,
        "attachment_url": msg.attachment_url,
        "filename": msg.filename,
        "timestamp": msg.timestamp.isoformat(),
        "status": "DELIVERED"
    }
    await manager.broadcast_to_group(msg_data, msg.group_id)
    
    return {"message": "Message edited"}

@router.delete("/groups/{group_id}")
def delete_group(
    group_id: str,
    user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    if user["role"] != "teacher":
        raise HTTPException(status_code=403, detail="Only teachers can delete groups")
    
    # Delete metadata
    meta = db.query(models.ChatGroupMetadata).filter(models.ChatGroupMetadata.group_id == group_id).first()
    if meta:
        db.delete(meta)
    
    # Delete messages
    msgs = db.query(models.ChatMessage).filter(models.ChatMessage.group_id == group_id).all()
    for m in msgs:
        db.delete(m)
        
    db.commit()
    return {"message": "Group deleted successfully"}

@router.put("/groups/{group_id}/metadata")
def update_group_metadata(
    group_id: str,
    req: schemas.ChatGroupMetadataUpdate,
    user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    if user["role"] != "teacher":
        raise HTTPException(status_code=403, detail="Only teachers can edit groups")
        
    meta = db.query(models.ChatGroupMetadata).filter(models.ChatGroupMetadata.group_id == group_id).first()
    if not meta:
        meta = models.ChatGroupMetadata(group_id=group_id, created_by=user["id"])
        db.add(meta)
        
    if req.name is not None:
        meta.name = req.name
        
    db.commit()
    return {"message": "Metadata updated successfully"}

@router.post("/groups/{group_id}/profile_image")
async def upload_group_image(
    group_id: str,
    file: UploadFile = File(...),
    user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    if user["role"] != "teacher":
        raise HTTPException(status_code=403, detail="Only teachers can upload group images")
        
    UPLOAD_DIR = "static/chat_profiles"
    os.makedirs(UPLOAD_DIR, exist_ok=True)
    
    file_path = os.path.join(UPLOAD_DIR, f"{group_id}_{file.filename}")
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    meta = db.query(models.ChatGroupMetadata).filter(models.ChatGroupMetadata.group_id == group_id).first()
    if not meta:
        meta = models.ChatGroupMetadata(group_id=group_id, created_by=user["id"])
        db.add(meta)
        
    meta.profile_image = f"/{file_path}"
    db.commit()
    
    return {"profile_image": meta.profile_image}
