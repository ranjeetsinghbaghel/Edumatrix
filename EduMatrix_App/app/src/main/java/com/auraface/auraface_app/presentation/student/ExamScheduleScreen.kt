package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScheduleScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadExamSchedule()
    }
    
    val exams = viewModel.examSchedule

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exams) { exam ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(exam.subject_name ?: "Unknown Subject", style = MaterialTheme.typography.titleMedium)
                        Text("Type: ${exam.exam_type}", style = MaterialTheme.typography.bodyMedium)
                        Text("Date: ${exam.date} | ${exam.start_time} - ${exam.end_time}", style = MaterialTheme.typography.bodyMedium)
                        Text("Room: ${exam.room}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (exams.isEmpty()) {
                item {
                    Text("No upcoming exams scheduled.")
                }
            }
        }
    }
}
