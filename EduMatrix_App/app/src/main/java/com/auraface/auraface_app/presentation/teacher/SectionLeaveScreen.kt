@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun SectionLeaveScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val leaves = viewModel.sectionLeaveRequests
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadSectionLeaves()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Section Leave Requests") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        if (leaves.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No pending leave requests for your section.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(leaves) { leave ->
                    val id = (leave["id"] as? Number)?.toInt() ?: 0
                    // Only rendering, assuming the list is already filtered by backend to only be "Pending_Class_Teacher"
                    LeaveRequestCard(
                        studentName = leave["student_name"]?.toString() ?: "Unknown",
                        rollNo = leave["roll_no"]?.toString() ?: "N/A",
                        reason = leave["reason"]?.toString() ?: "No reason provided",
                        date = leave["date"]?.toString() ?: "",
                        status = leave["status"]?.toString() ?: "Unknown",
                        isHodView = false,
                        onAction = { 
                            viewModel.approveSectionLeave(id) {
                                viewModel.loadSectionLeaves()
                            }
                        },
                        actionLabel = "Approve",
                        actionIcon = Icons.Filled.Check,
                        isHod = true,
                        onReject = { 
                            viewModel.rejectSectionLeave(id) {
                                viewModel.loadSectionLeaves()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LeaveRequestCard(
    studentName: String,
    rollNo: String,
    reason: String,
    date: String,
    status: String,
    isHodView: Boolean,
    onAction: () -> Unit,
    actionLabel: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isHod: Boolean = false, // Kept for backward compat param name if needed, but we use isHodView mostly
    onReject: (() -> Unit)? = null
) {
    // Determine if action is needed
    val showActions = if (isHodView) {
        status == "Pending_HOD"
    } else {
        status == "Pending_Class_Teacher"
    }
    
    val statusText = when(status) {
        "Pending_Class_Teacher" -> "Pending Class Teacher Approval"
        "Pending_HOD" -> "Forwarded to HOD"
        "Approved_By_HOD" -> "Approved"
        "Rejected_By_Class_Teacher" -> "Rejected by Class Teacher"
        "Rejected_By_HOD" -> "Rejected by HOD"
        else -> status
    }
    
    val statusColor = when {
        status.contains("Approved") -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        status.contains("Rejected") -> androidx.compose.ui.graphics.Color(0xFFEF5350)
        status.contains("Forwarded") || status.contains("Pending") -> androidx.compose.ui.graphics.Color(0xFFFFA726)
        else -> androidx.compose.ui.graphics.Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(studentName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Roll No: $rollNo | Date: $date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                if (!showActions) {
                     Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Reason:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(reason, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAction,
                        modifier = Modifier.weight(1f),
                        enabled = if (isHodView) status == "Pending_HOD" else status == "Pending_Class_Teacher",
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(actionIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(actionLabel, fontSize = 12.sp)
                    }
                    
                    if (onReject != null) {
                        Button(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reject", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
