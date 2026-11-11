package com.auraface.auraface_app.core.utils

import android.annotation.SuppressLint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

object FaceDetectorUtil {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()

        FaceDetection.getClient(options)
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun detectFace(
        imageProxy: androidx.camera.core.ImageProxy,
        onFaceDetected: () -> Unit
    ) {
        val mediaImage = imageProxy.image ?: return

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    onFaceDetected()
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
