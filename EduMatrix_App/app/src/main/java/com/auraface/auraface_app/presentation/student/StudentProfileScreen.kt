package com.auraface.auraface_app.presentation.student

import android.content.Intent
import android.net.Uri

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.auraface.auraface_app.data.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profile = viewModel.profile
    val scrollState = rememberScrollState()
    var showBloodGroupDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    if (showBloodGroupDialog) {
        BloodGroupUpdateDialog(
            currentBloodGroup = profile?.blood_group ?: "",
            onDismiss = { showBloodGroupDialog = false },
            onUpdate = { newBloodGroup ->
                viewModel.updateBloodGroup(
                    bloodGroup = newBloodGroup,
                    onSuccess = { showBloodGroupDialog = false },
                    onError = { /* Handle error gracefully */ }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    TextButton(onClick = { 
                        try { navController.popBackStack() } catch(e: Exception) { e.printStackTrace() }
                    }) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        try {
                            viewModel.loadProfile()
                        } catch(e: Exception) { e.printStackTrace() }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.profile == null && viewModel.error == null) {
             Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator()
             }
        } else if (profile != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.profile_image != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(RetrofitClient.BASE_URL + profile.profile_image.removePrefix("/"))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = profile.name?.take(1) ?: profile.username.take(1) ?: "S",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = profile.name ?: profile.username,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.roll_no ?: "No Roll Number", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Personal Details
                ProfileSectionCard(title = "Personal Details") {
                    ProfileRow(icon = Icons.Default.Email, label = "Email", value = profile.email)
                    ProfileRow(icon = Icons.Default.Phone, label = "Mobile", value = profile.mobile)
                    ProfileRow(icon = Icons.Default.Home, label = "Address", value = profile.address)
                    ProfileRow(
                        icon = Icons.Default.Bloodtype, 
                        label = "Blood Group", 
                        value = profile.blood_group,
                        onClick = { showBloodGroupDialog = true }
                    )
                    ProfileRow(icon = Icons.Default.Person, label = "Username", value = profile.username)
                }

                // Academic Details
                ProfileSectionCard(title = "Academic Details") {
                    ProfileRow(icon = Icons.Default.School, label = "Department", value = profile.department)
                    val yearStr = profile.year?.let { 
                        when(it) {
                            1 -> "1st Year"
                            2 -> "2nd Year"
                            3 -> "3rd Year"
                            4 -> "4th Year"
                            else -> "$it Year"
                        }
                    }
                    ProfileRow(icon = Icons.Default.DateRange, label = "Year", value = yearStr)
                    ProfileRow(icon = Icons.Default.Class, label = "Semester", value = profile.semester?.toString())
                    ProfileRow(icon = Icons.Default.Group, label = "Section", value = profile.section)
                }

                // Guardian Details
                if (profile.guardian_name != null || profile.guardian_mobile != null) {
                    ProfileSectionCard(title = "Guardian Details") {
                        ProfileRow(icon = Icons.Default.Person, label = "Guardian Name", value = profile.guardian_name)
                        ProfileRow(icon = Icons.Default.Phone, label = "Guardian Mobile", value = profile.guardian_mobile)
                        ProfileRow(icon = Icons.Default.Email, label = "Guardian Email", value = profile.guardian_email)
                        
                        if (!profile.guardian_mobile.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.guardian_mobile}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call Guardian (${profile.guardian_mobile})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                
                // Academic Ledger & Records Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Academic Ledger & Records", fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                    Text("Institutional Database Records", style = MaterialTheme.typography.bodySmall, color = Color(0xFF15803D))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text("VERIFIED", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color(0xFF16A34A), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Text(
                            "View your semester attendance breakdown, internal assessment marks, fee ledger, and academic standings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                        Button(
                            onClick = {
                                navController.navigate(com.auraface.auraface_app.core.navigation.Screen.GietErpDashboard.route)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Academic Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("change_password/student") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Password")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            // Error case
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                 Text("Failed to load profile. Please try again.")
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = if (onClick != null) 4.dp else 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value ?: "N/A",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BloodGroupUpdateDialog(
    currentBloodGroup: String,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit
) {
    var bloodGroup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentBloodGroup) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Update Blood Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "e.g., O+, A-, B+, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(bloodGroup.trim()) },
                enabled = bloodGroup.trim().isNotEmpty()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


