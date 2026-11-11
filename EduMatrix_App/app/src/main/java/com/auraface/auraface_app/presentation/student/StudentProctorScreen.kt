package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProctorScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val meetings = viewModel.proctorMeetings
    
    // Dialog state
    var showResponseDialog by remember { mutableStateOf(false) }
    var meetingIdToRespond by remember { mutableStateOf<Int?>(null) }
    var responseText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadProctorMeetings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proctor History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(meetings) { meeting ->
                Card(
                     modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Date: ${meeting.date}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Remarks: ${meeting.remarks}", style = MaterialTheme.typography.bodyMedium)
                        
                        if (meeting.action_taken != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Action: ${meeting.action_taken}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        
                        // Student Response Section
                        if (meeting.student_response != null) {
                             Spacer(modifier = Modifier.height(8.dp))
                             Text("Your Notes: ${meeting.student_response}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary)
                        } else {
                             Spacer(modifier = Modifier.height(8.dp))
                             OutlinedButton(onClick = {
                                 meetingIdToRespond = meeting.id
                                 responseText = ""
                                 showResponseDialog = true
                             }) {
                                 Text("Add Discussion Notes")
                             }
                        }
                    }
                }
            }
            if (meetings.isEmpty()) {
                item {
                    Text("No proctor meetings recorded.", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
    
    if (showResponseDialog && meetingIdToRespond != null) {
        AlertDialog(
            onDismissRequest = { showResponseDialog = false },
            title = { Text("Add Discussion Notes") },
            text = {
                OutlinedTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = { Text("Notes/Response") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (responseText.isNotBlank()) {
                         viewModel.respondToProctorMeeting(meetingIdToRespond!!, responseText) {
                             showResponseDialog = false
                         }
                    }
                }) {
                    Text("Save")
                }
            },
             dismissButton = {
                TextButton(onClick = { showResponseDialog = false }) { Text("Cancel") }
            }
        )
    }
}
