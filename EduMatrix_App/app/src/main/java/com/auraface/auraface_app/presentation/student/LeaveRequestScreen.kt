@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun LeaveRequestScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadLeaves()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apply for Leave") },
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
                .padding(24.dp)
        ) {
            Text(
                "Submit your leave request. This will be reviewed by your department faculty.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var leaveType by remember { mutableStateOf("Sick Leave") }
            var expanded by remember { mutableStateOf(false) }
            val leaveTypes = listOf("Sick Leave", "Casual Leave", "On Duty", "Event", "Other")

            Text("Leave Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Leave Type Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = leaveType,
                    onValueChange = { },
                    label = { Text("Leave Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    leaveTypes.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                leaveType = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Detailed Reason") },
                placeholder = { Text("Explain your problem here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 10
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text("My Requests", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.leaveRequests) { leave ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val statusText = when(leave["status"]) {
                                    "Pending_Class_Teacher" -> "Pending Class Teacher"
                                    "Pending_HOD" -> "Approved by Class Teacher (Pending HOD)"
                                    "Approved_By_HOD" -> "Accepted by HOD"
                                    "Rejected_By_Class_Teacher" -> "Rejected by Class Teacher"
                                    "Rejected_By_HOD" -> "Rejected by HOD"
                                    else -> leave["status"].toString()
                                }
                                
                                val statusColor = when(leave["status"]) {
                                    "Approved_By_HOD" -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
                                    "Pending_HOD" -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
                                    "Rejected_By_Class_Teacher", "Rejected_By_HOD" -> MaterialTheme.colorScheme.error
                                    else -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange (Pending)
                                }

                                Text(statusText, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 12.sp)
                                val rawDate = leave["date"]?.toString() ?: ""
                                // Try to format date if it's in YYYY-MM-DD
                                Text(rawDate, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(leave["reason"]?.toString() ?: "", fontSize = 14.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (reason.length < 5) {
                         Toast.makeText(context, "Please provide a detailed reason", Toast.LENGTH_SHORT).show()
                    } else {
                        // Format: [Type]-Reason
                        val fullReason = "[$leaveType]-$reason"
                        viewModel.submitLeave(fullReason) {
                            Toast.makeText(context, "Leave Request Sent!", Toast.LENGTH_SHORT).show()
                            // Clear fields instead of popping stack to allow more
                            reason = ""
                            viewModel.loadLeaves() // Reload to show new one
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Request", fontWeight = FontWeight.Bold)
            }
        }
    }
}
