package com.auraface.auraface_app.core.utils

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val onFaceStatus: (faceCount: Int, isLive: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // Landmarks help in accuracy
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        onFaceStatus(0, false)
                    } else {
                        // Strict Liveness Check to prevent photo Spoofing
                        // A photo cannot easily change expression or rotate head in 3D
                        val isAnyLive = faces.any { face ->
                             val smiling = (face.smilingProbability ?: 0f) > 0.6f
                             val headTurn = kotlin.math.abs(face.headEulerAngleY) > 10f // Left/Right turn (Yaw)
                             val headTilt = kotlin.math.abs(face.headEulerAngleZ) > 10f // Sideways tilt (Roll)
                             val headNod = kotlin.math.abs(face.headEulerAngleX) > 10f // Up/Down nod (Pitch)
                             
                             smiling || headTurn || headTilt || headNod
                        }
                        onFaceStatus(faces.size, isAnyLive)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
