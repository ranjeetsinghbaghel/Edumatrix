import os
import numpy as np
import re
import time

# Calculate absolute path to avoiding CWD issues
# recognize.py is in app/face_recognition
# We want PROJECT_ROOT/images/encodings
CURRENT_FILE = os.path.abspath(__file__)
APP_DIR = os.path.dirname(os.path.dirname(CURRENT_FILE)) # .../app
PROJECT_ROOT = os.path.dirname(APP_DIR) # .../AuraFace
ENCODINGS_DIR = os.path.join(PROJECT_ROOT, "images", "encodings")
STUDENTS_DIR = os.path.join(PROJECT_ROOT, "images", "students")

# Global state
KNOWN_ENCODINGS = []
KNOWN_STUDENT_IDS = []

def load_known_faces():
    global KNOWN_ENCODINGS, KNOWN_STUDENT_IDS
    
    # Clear existing
    KNOWN_ENCODINGS.clear()
    KNOWN_STUDENT_IDS.clear()

    # Ensure directories exist
    if not os.path.exists(STUDENTS_DIR):
        print(f"Warning: Students directory not found at {STUDENTS_DIR}")
        return

    os.makedirs(ENCODINGS_DIR, exist_ok=True)

    count = 0
    loaded_ids = []
    
    print(f"Scanning student images from: {STUDENTS_DIR}")
    
    # Optimize: Check if we need face_recognition (only import if needed to avoid startup lag if cached)
    # However, for simplicity and ensuring it's available, we might import it inside the loop or check first.
    # We'll import it at the top of the loop if we detect missing encodings.
    face_recognition = None

    for file in os.listdir(STUDENTS_DIR):
        if not (file.lower().endswith('.jpg') or file.lower().endswith('.jpeg') or file.lower().endswith('.png')):
            continue

        # Extract ID (Suffix of filename to match 23CSE706 -> 706)
        name_stem = os.path.splitext(file)[0]
        match = re.search(r"(\d+)$", name_stem)
        if match:
            student_id = int(match.group(1)) 
        else:
            print(f"Skipping {file}: No trailing digits found in filename")
            continue

        full_img_path = os.path.join(STUDENTS_DIR, file)
        npy_path = os.path.join(ENCODINGS_DIR, f"{student_id}.npy")
        
        encoding = None
        
        # 1. Try Loading Cache
        if os.path.exists(npy_path):
            # Check if cache is up-to-date (npy newer than image)
            if os.path.getmtime(npy_path) > os.path.getmtime(full_img_path):
                try:
                    encoding = np.load(npy_path)
                except Exception as e:
                    print(f"Corrupt cache for {student_id}, regenerating. Error: {e}")
        
        # 2. Generate if missing/outdated
        if encoding is None:
            if face_recognition is None:
                print("Importing face_recognition module (this may take a moment)...")
                import face_recognition

            print(f"Generating encoding for {file}...")
            try:
                image = face_recognition.load_image_file(full_img_path)
                encodings = face_recognition.face_encodings(image)
                
                if len(encodings) > 0:
                    encoding = encodings[0]
                    # Save to cache
                    np.save(npy_path, encoding)
                else:
                    print(f"No face found in {file}. Skipping.")
            except Exception as e:
                print(f"Error processing {file}: {e}")

        # 3. Add to memory
        if encoding is not None:
            KNOWN_ENCODINGS.append(encoding)
            KNOWN_STUDENT_IDS.append(student_id)
            loaded_ids.append(student_id)
            count += 1

    print(f"Loaded {count} known faces. IDs: {loaded_ids}")

def add_or_update_face(student_id, encoding):
    """
    Updates the in-memory known faces list with the new encoding for the given student_id.
    If the student_id already exists, its encoding is updated.
    If it's new, it's appended.
    """
    global KNOWN_ENCODINGS, KNOWN_STUDENT_IDS
    
    try:
        if student_id in KNOWN_STUDENT_IDS:
            index = KNOWN_STUDENT_IDS.index(student_id)
            KNOWN_ENCODINGS[index] = encoding
            print(f"🔄 Updated encoding for student {student_id}")
        else:
            KNOWN_STUDENT_IDS.append(student_id)
            KNOWN_ENCODINGS.append(encoding)
            print(f"➕ Added new encoding for student {student_id}")
    except Exception as e:
        print(f"❌ Failed to update in-memory face cache: {e}")
