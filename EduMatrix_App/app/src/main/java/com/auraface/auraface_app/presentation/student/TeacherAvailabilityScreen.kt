package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAvailabilityScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTeacher by remember { mutableStateOf<com.auraface.auraface_app.data.network.api.TeacherAvailabilityDTO?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val teachersList = viewModel.teacherAvailability

    LaunchedEffect(Unit) {
        viewModel.loadTeacherAvailability()
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Teacher Availability", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(teachersList) { teacher ->
                    TeacherAvailabilityCard(teacher) {
                        selectedTeacher = teacher
                        showDialog = true
                    }
                }
            }
        }
    }

    if (showDialog && selectedTeacher != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Request Meeting") },
            text = { Text("Send a meeting request to ${selectedTeacher?.name}?") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    val myId = viewModel.profile?.id ?: 0
                    val theirId = selectedTeacher?.id?.toIntOrNull() ?: 0
                    if (myId != 0 && theirId != 0) {
                        val groupId = "DM_${minOf(myId, theirId)}_${maxOf(myId, theirId)}"
                        navController.navigate("chat_main?groupId=$groupId&groupName=${selectedTeacher?.name}")
                    }
                }) {
                    Text("Message")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TeacherAvailabilityCard(teacher: com.auraface.auraface_app.data.network.api.TeacherAvailabilityDTO, onRequest: () -> Unit) {
    val statusColor = when (teacher.status) {
        "In Class" -> Color(0xFFFF9800)
        "8 AM to 7 PM" -> Color(0xFF4CAF50)
        else -> Color(0xFF2196F3) // Use blue for custom availability messages
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(teacher.department, color = Color.Gray, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(teacher.status, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (teacher.status != "In Class") {
                Button(onClick = onRequest, shape = RoundedCornerShape(8.dp)) {
                    Text("Meet")
                }
            }
        }
    }
}
