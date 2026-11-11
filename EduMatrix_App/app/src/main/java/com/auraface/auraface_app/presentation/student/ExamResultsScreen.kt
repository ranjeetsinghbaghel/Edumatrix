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
fun ExamResultsScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadResults()
    }
    
    val results = viewModel.results

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam Results") },
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
            items(results) { res ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(res.subject_name ?: res.subject_id, style = MaterialTheme.typography.titleMedium)
                            Text(res.assessment_type, style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Text(
                            "${res.score} / ${res.total_marks}",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            if (results.isEmpty()) {
                item { Text("No results found.") }
            }
        }
    }
}
