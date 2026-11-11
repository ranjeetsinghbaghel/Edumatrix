
import os
import time

def resolve_profile_image(user, student=None):
    """
    Resolves the profile image URL for a user/student, checking for existence 
    and appending a cache-busting timestamp.
    """
    image_path = None
    
    # 1. Check Explicit User Profile Image
    if user.profile_image:
        # Ensure it exists on disk before returning
        # user.profile_image stored as "/images/..."
        local_path = user.profile_image.lstrip("/")
        if os.path.exists(local_path):
            image_path = user.profile_image
    
    # 2. Fallback: Check standard student path
    if not image_path and student and hasattr(student, 'roll_no') and student.roll_no:
        s_path = f"images/students/{student.roll_no}.jpg"
        if os.path.exists(s_path):
            image_path = f"/{s_path}"
    
    # 3. Fallback: Check standard username path
    if not image_path and hasattr(user, 'username'):
        u_path = f"images/registered/{user.username}.jpg"
        if os.path.exists(u_path):
            image_path = f"/{u_path}"
    
    # Append Cache-Busting Timestamp
    if image_path:
        try:
            local_path = image_path.lstrip("/")
            if os.path.exists(local_path):
                mtime = int(os.path.getmtime(local_path))
                return f"{image_path}?v={mtime}"
        except Exception:
            pass
        return image_path
            
    return None
