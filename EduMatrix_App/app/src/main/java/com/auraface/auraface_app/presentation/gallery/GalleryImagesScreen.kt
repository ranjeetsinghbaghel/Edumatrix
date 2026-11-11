package com.auraface.auraface_app.presentation.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.auraface.auraface_app.data.remote.dto.GalleryImageDto
import com.auraface.auraface_app.data.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryImagesScreen(
    navController: NavController,
    folderId: Int,
    folderName: String,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val role = viewModel.getRole()
    
    // Upload Dialog
    var showUploadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(folderId) {
        viewModel.loadImages(folderId)
    }

    // Full Screen Viewer State
    var selectedImage by remember { mutableStateOf<GalleryImageDto?>(null) }

    if (selectedImage != null) {
        FullScreenImageViewer(
            imageUrl = selectedImage!!.image_url,
            onDismiss = { selectedImage = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (role == "admin" || role == "teacher") {
                        IconButton(onClick = { showUploadDialog = true }) {
                            Icon(Icons.Default.Upload, contentDescription = "Upload Image")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uploadProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            
            if (isLoading && images.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (images.isEmpty()) {
                Text(
                    text = "No images in this folder.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { img ->
                        ImageThumbnail(
                           image = img,
                           role = role,
                           onApprove = { viewModel.approveImage(img.id, folderId) },
                           onDelete = { viewModel.deleteImage(img.id, folderId) },
                           onClick = { selectedImage = img }
                        )
                    }
                }
            }
        }
    }
    
    if (showUploadDialog) {
        ImageUploadDialog(
            onDismiss = { showUploadDialog = false },
            onConfirm = { file -> 
                viewModel.uploadImage(folderId, file)
                showUploadDialog = false
            }
        )
    }
}

@Composable
fun ImageThumbnail(
    image: GalleryImageDto,
    role: String,
    onApprove: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
         modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
         shape = RoundedCornerShape(8.dp),
         elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = RetrofitClient.BASE_URL + image.image_url.removePrefix("/"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Status Overlay
            if (image.status == "PENDING") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .align(Alignment.BottomCenter)
                        .padding(4.dp)
                ) {
                    Text(
                        "Pending", 
                        color = Color.Yellow, 
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            // Admin Controls
            if (role == "admin") {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(bottomStart = 8.dp))
                ) {
                    if (image.status == "PENDING") {
                        IconButton(onClick = onApprove, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Check, null, tint = Color.Green)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun ImageUploadDialog(
    onDismiss: () -> Unit,
    onConfirm: (java.io.File) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Image") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                           model = selectedImageUri,
                           contentDescription = null,
                           contentScale = ContentScale.Crop,
                           modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp))
                    }
                }
                Text("Tap to select image")
            }
        },
        confirmButton = {
            Button(
                enabled = selectedImageUri != null,
                onClick = {
                    selectedImageUri?.let { uri ->
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val file = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                        val outputStream = java.io.FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                        onConfirm(file)
                    }
                }
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val scale = remember { mutableStateOf(1f) }
        val offset = remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() } // Tap anywhere to dismiss
        ) {
            AsyncImage(
                model = RetrofitClient.BASE_URL + imageUrl.removePrefix("/"),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    // Simple zoom implementation
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale.value *= zoom
                            offset.value += pan
                        }
                    }
                    .graphicsLayer(
                        scaleX = maxOf(1f, scale.value),
                        scaleY = maxOf(1f, scale.value),
                        translationX = offset.value.x,
                        translationY = offset.value.y
                    )
            )
             
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
