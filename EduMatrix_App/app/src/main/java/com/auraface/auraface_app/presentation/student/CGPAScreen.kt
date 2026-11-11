package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CGPAScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadCGPA()
    }
    
    val cgpa = viewModel.cgpa

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My CGPA") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top, // Changed from Center to Top for better scroll
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cgpa != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("CGPA", style = MaterialTheme.typography.displayMedium)
                Text("${cgpa.cgpa}", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Total Points: ${cgpa.total_points}")
                Text("Total Credits: ${cgpa.total_credits}")

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                // Manually handling state without 'by' delegation to avoid imports
                val inputForPercentageState = remember { mutableStateOf("") }
                val calculatedPercentageState = remember { mutableStateOf("") }

                val inputForCgpaState = remember { mutableStateOf("") }
                val calculatedCgpaState = remember { mutableStateOf("") }

                Text("Tools", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // CGPA to Percentage
                OutlinedTextField(
                    value = inputForPercentageState.value,
                    onValueChange = { 
                        inputForPercentageState.value = it
                        val value = it.toDoubleOrNull()
                        if (value != null) {
                            calculatedPercentageState.value = "%.2f %%".format(value * 9.5)
                        } else {
                            calculatedPercentageState.value = ""
                        }
                    },
                    label = { Text("Enter CGPA") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (calculatedPercentageState.value.isNotEmpty()) {
                    Text("Percentage: ${calculatedPercentageState.value}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Percentage to CGPA
                 OutlinedTextField(
                    value = inputForCgpaState.value,
                    onValueChange = { 
                        inputForCgpaState.value = it
                        val value = it.toDoubleOrNull()
                        if (value != null) {
                            calculatedCgpaState.value = "%.2f".format(value / 9.5)
                        } else {
                            calculatedCgpaState.value = ""
                        }
                    },
                    label = { Text("Enter Percentage") },
                    modifier = Modifier.fillMaxWidth()
                )
                 if (calculatedCgpaState.value.isNotEmpty()) {
                    Text("CGPA: ${calculatedCgpaState.value}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.height(32.dp))

            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Calculating...", modifier = Modifier.padding(top=8.dp))
                    }
                }
            }
        }
    }
}
