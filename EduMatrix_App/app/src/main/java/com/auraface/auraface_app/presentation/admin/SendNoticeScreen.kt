@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun SendNoticeScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("ALL") } // ALL, STUDENTS, TEACHERS
    var priority by remember { mutableStateOf("LOW") } // HIGH, MEDIUM, LOW
    
    var targetExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    
    // Targeting State
    var deptExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var selectedDept by remember { mutableStateOf("All Depts") }
    var selectedYear by remember { mutableStateOf("All Years") }
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send New Notice") },
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
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "Broadcast a Message",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "This will send a push notification and add to the notice board.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Notice Title") },
                placeholder = { Text("e.g. Holiday Announcement") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Notice Message") },
                placeholder = { Text("Describe the notice details...") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Target Audience
                Column(modifier = Modifier.weight(1f)) {
                    Text("Audience", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { targetExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(targetAudience)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = targetExpanded,
                            onDismissRequest = { targetExpanded = false }
                        ) {
                            listOf("ALL", "STUDENTS", "TEACHERS").forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        targetAudience = it
                                        targetExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Priority
                Column(modifier = Modifier.weight(1f)) {
                    Text("Priority", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { priorityExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(priority)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false }
                        ) {
                            listOf("LOW", "MEDIUM", "HIGH").forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        priority = it
                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (targetAudience == "STUDENTS") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Department
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Department", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                        Box {
                             OutlinedButton(
                                onClick = { deptExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(selectedDept, maxLines = 1)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = deptExpanded,
                                onDismissRequest = { deptExpanded = false }
                            ) {
                                listOf("All Depts", "CSE", "ECE", "MECH", "CIVIL").forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            selectedDept = it
                                            deptExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Year
                     Column(modifier = Modifier.weight(1f)) {
                        Text("Year", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                        Box {
                             OutlinedButton(
                                onClick = { yearExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(selectedYear, maxLines = 1)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                listOf("All Years", "1", "2", "3", "4").forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            selectedYear = it
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                 Spacer(modifier = Modifier.height(0.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (title.isBlank() || message.isBlank()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    viewModel.sendNotice(
                        title = title,
                        message = message,
                        target = targetAudience,
                        priority = priority,
                        department = if (targetAudience == "STUDENTS") selectedDept else "All Depts",
                        year = if (targetAudience == "STUDENTS") selectedYear else "All Years",
                        onSuccess = {
                            Toast.makeText(context, "Notice sent successfully!", Toast.LENGTH_LONG).show()
                            navController.popBackStack()
                        },
                        onError = {
                             Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Send Notice Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
