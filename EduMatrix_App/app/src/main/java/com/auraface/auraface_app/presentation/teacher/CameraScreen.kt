package com.auraface.auraface_app.presentation.teacher


import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.auraface.auraface_app.core.utils.ImageUtils
import com.auraface.auraface_app.core.utils.imageProxyToBitmap

@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFaceDetected by remember { mutableStateOf(false) }
    var facesCount by remember { mutableStateOf(0) }
    var isLivenessConfirmed by remember { mutableStateOf(false) }
    var cameraLens by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    com.auraface.auraface_app.core.utils.FaceAnalyzer { count, live ->
                        facesCount = count
                        isFaceDetected = count > 0 // Keep basic boolean for simplicity elsewhere
                        
                        // Rule: Liveness Bypass for Groups or Back Camera
                        // If multiple faces are present (Group Attendance), we skip strict liveness (hard to coordinate 3 people creating angles)
                        // If Back Camera is used, we assume teacher is taking photo (skip strict check)
                        val isBackCamera = (cameraLens == CameraSelector.DEFAULT_BACK_CAMERA)
                        val isGroup = count > 1
                        
                        if (isGroup || isBackCamera) {
                            isLivenessConfirmed = true
                        } else if (count == 1 && live) {
                            isLivenessConfirmed = true
                        } else {
                            // If count == 1 and NOT live, we reset to false (unless it was previously true? No, real-time check)
                            // Actually, keeping it true once verified is better UX, but for security, real-time is safer.
                            // Let's stick to real-time.
                            isLivenessConfirmed = false
                        }
                    }
                )
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
             factory = { ctx ->
                 val previewView = PreviewView(ctx)
                 previewView
             },
             update = { previewView ->
                 val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                 cameraProviderFuture.addListener({
                     val cameraProvider = cameraProviderFuture.get()
                     val preview = Preview.Builder().build().apply {
                         setSurfaceProvider(previewView.surfaceProvider)
                     }

                     try {
                         cameraProvider.unbindAll()
                         cameraProvider.bindToLifecycle(
                             lifecycleOwner,
                             cameraLens,
                             preview,
                             imageCapture,
                             imageAnalysis
                         )
                     } catch (e: Exception) {
                         e.printStackTrace()
                     }
                 }, ContextCompat.getMainExecutor(context))
             }
         )

        // Status Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isFaceDetected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) 
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
            ) {
                Text(
                    text = if (facesCount > 0) "✅ $facesCount Face(s) Detected" else "❌ Position face(s) in frame",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isLivenessConfirmed) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f) 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Text(
                    text = when {
                        facesCount > 1 -> "✨ Group Detected - Ready"
                        isLivenessConfirmed -> "✨ Liveness Verified"
                        else -> "☝ Please Smile, Nod, or Tilt Head"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Camera Switch Button (Top Right)
        IconButton(
            onClick = {
                cameraLens = if (cameraLens == CameraSelector.DEFAULT_FRONT_CAMERA) 
                             CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                isLivenessConfirmed = false // Reset liveness on switch
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape)
        ) {
             // Fallback icon if Cameraswitch doesn't exist in standard set, using Refresh which is common
             // or try to use proper icon if available
             Icon(Icons.Default.Refresh, contentDescription = "Switch Camera", tint = MaterialTheme.colorScheme.onSurface)
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth(0.8f),
            enabled = isFaceDetected && isLivenessConfirmed,
            onClick = {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            val base64 = ImageUtils.bitmapToBase64(bitmap)
                            image.close()
                            onImageCaptured(base64)
                        }
                    }
                )
            }
        ) {
            Text(if (isLivenessConfirmed) "Mark Attendance" else "Verify Liveness First")
        }
    }
}
