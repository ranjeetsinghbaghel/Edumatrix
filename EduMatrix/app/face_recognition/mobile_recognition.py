import base64
import cv2
import numpy as np
import face_recognition
import time
import os
from app.face_recognition.utils import mark_attendance

ENCODINGS_DIR = "images/encodings"
COOLDOWN_SECONDS = 3600  # 1 hour
last_marked = {}


def load_known_faces():
    encodings = []
    names = []

    for file in os.listdir(ENCODINGS_DIR):
        if file.endswith(".npy"):
            encodings.append(np.load(os.path.join(ENCODINGS_DIR, file)))
            names.append(file.replace(".npy", ""))

    return encodings, names


known_encodings, known_names = load_known_faces()


def recognize_from_base64(base64_image: str):
    try:
        image_bytes = base64.b64decode(base64_image)
        np_arr = np.frombuffer(image_bytes, np.uint8)
        frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        locations = face_recognition.face_locations(rgb)
        encodings = face_recognition.face_encodings(rgb, locations)

        if not encodings:
            return {"status": "fail", "message": "No face detected"}

        results = []
        for encoding in encodings:
            distances = face_recognition.face_distance(known_encodings, encoding)
            best_index = np.argmin(distances)
            best_distance = distances[best_index]

            if best_distance < 0.55:
                name = known_names[best_index]
                now = time.time()
                last_time = last_marked.get(name, 0)

                if now - last_time >= COOLDOWN_SECONDS:
                    result_msg = mark_attendance(name)
                    last_marked[name] = now
                    results.append({"status": "success", "name": name, "message": result_msg})
                else:
                    results.append({"status": "already_marked", "name": name})
            else:
                results.append({"status": "unknown"})
        
        # If any success, return success summary
        success_names = [r["name"] for r in results if r["status"] == "success"]
        if success_names:
             return {"status": "success", "name": ", ".join(success_names), "message": "Attendance Marked", "details": results}

        return {"status": "mixed", "details": results}

    except Exception as e:
        return {"status": "error", "message": str(e)}
