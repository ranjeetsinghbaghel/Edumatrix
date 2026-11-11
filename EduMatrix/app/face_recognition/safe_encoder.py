import dlib
import numpy as np
import face_recognition_models

predictor_path = face_recognition_models.pose_predictor_model_location()
face_rec_model_path = face_recognition_models.face_recognition_model_location()

detector = dlib.get_frontal_face_detector()
sp = dlib.shape_predictor(predictor_path)
facerec = dlib.face_recognition_model_v1(face_rec_model_path)


def get_face_encodings(rgb_image):
    encodings = []
    faces = detector(rgb_image, 1)

    for face in faces:
        shape = sp(rgb_image, face)

        # 🔥 SAFE CALL (NO LANDMARK SET PASSING)
        encoding = np.array(
            facerec.compute_face_descriptor(rgb_image)
        )

        encodings.append(encoding)

    return encodings, faces
