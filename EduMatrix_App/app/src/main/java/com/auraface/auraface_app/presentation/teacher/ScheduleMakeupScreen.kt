@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@Composable
fun ScheduleMakeupScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    var date by remember { mutableStateOf("") }
    var timeSlot by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Make-up Class") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = timeSlot, onValueChange = { timeSlot = it }, label = { Text("Time (HH:MM-HH:MM)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = subjectId, onValueChange = { subjectId = it }, label = { Text("Subject ID/Name") }, modifier = Modifier.fillMaxWidth())
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Dept") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Sem") }, modifier = Modifier.weight(1f))
            }

            OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room (Optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason (Optional)") }, modifier = Modifier.fillMaxWidth())

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            if (successMessage != null) {
                Text(successMessage!!, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = {
                    errorMessage = null
                    successMessage = null
                    viewModel.requestMakeupClass(
                        date, timeSlot, subjectId, department, year.toIntOrNull() ?: 0, 
                        semester.toIntOrNull() ?: 0, section, room.ifBlank{ null }, reason.ifBlank{ null },
                        onSuccess = {
                            successMessage = "Class scheduled successfully!"
                            // Clear fields or navigate back
                        },
                        onError = { errorMessage = it }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Schedule Class")
            }
        }
    }
}
