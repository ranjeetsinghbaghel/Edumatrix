@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.teacher

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.auraface.auraface_app.data.network.RetrofitClient
import com.auraface.auraface_app.presentation.common.TimetableList

@Composable
fun TeacherProfileScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val profile = viewModel.profile
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }
    val scrollState = rememberScrollState()
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    var availabilityMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.loadProfile()
                        viewModel.loadDashboardData()
                        viewModel.loadTimetable()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (profile != null) {
                // Profile Header
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.profile_image != null) {
                       AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(RetrofitClient.BASE_URL + profile.profile_image!!.removePrefix("/"))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(profile.name ?: profile.username, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (profile.name != null) {
                    Text("@${profile.username}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(profile.position ?: profile.role.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileInfoRow("Position", profile.position ?: "Not set", Icons.Default.Star)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        ProfileInfoRow("Mobile", profile.mobile ?: "Not set", Icons.Default.Call)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        ProfileInfoRow("Email", profile.email ?: "Not set", Icons.Default.Email)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        ProfileInfoRow("Address", profile.address ?: "Not set", Icons.Default.Home)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        ProfileInfoRow("Qualification", profile.qualification ?: "Not set", Icons.Default.Info)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        ProfileInfoRow("Username", profile.username, Icons.Default.AccountBox)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Availability Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Current Status", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Switch(
                                checked = viewModel.dashboardData?.isAvailable ?: true,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateAvailabilityToggle(isChecked)
                                }
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("My Default Availability", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            IconButton(onClick = { 
                                availabilityMessage = viewModel.dashboardData?.customAvailabilityMessage ?: "8 AM to 7 PM"
                                showAvailabilityDialog = true 
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Availability")
                            }
                        }
                        Text(
                            text = viewModel.dashboardData?.customAvailabilityMessage ?: "8 AM to 7 PM",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Assigned Subjects", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val assigned = viewModel.dashboardData?.assignedSubjects ?: emptyList()
                if (assigned.isEmpty()) {
                    Text("No subjects assigned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    assigned.forEach { sub ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (sub.department != null) {
                                    Text("Class: ${sub.department} ${sub.year ?: ""} ${if (sub.semester != null) "Sem-${sub.semester}" else ""} ${sub.section ?: ""}", 
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Text("ID: ${sub.id}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "My Teaching Schedule", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                com.auraface.auraface_app.presentation.common.PremiumTimetable(
                    timetable = viewModel.timetable,
                    department = null, // Teacher view covers multiple classes
                    year = null,
                    section = null
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { navController.navigate("change_password/teacher") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Password")
                }

            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showAvailabilityDialog) {
        AlertDialog(
            onDismissRequest = { showAvailabilityDialog = false },
            title = { Text("Update Availability") },
            text = {
                OutlinedTextField(
                    value = availabilityMessage,
                    onValueChange = { availabilityMessage = it },
                    label = { Text("Availability Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showAvailabilityDialog = false
                    viewModel.updateAvailability(availabilityMessage, onSuccess = {}, onError = {})
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAvailabilityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
