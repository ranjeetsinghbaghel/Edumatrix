@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun HodLeaveScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val leaves = viewModel.hodLeaveRequests
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadHodLeaves()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Department Leave Requests") },
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
                Text("No pending leave requests for the department.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(leaves) { leave ->
                    val id = (leave["id"] as? Number)?.toInt() ?: 0
                    LeaveRequestCard(
                        studentName = leave["student_name"]?.toString() ?: "Unknown",
                        rollNo = leave["roll_no"]?.toString() ?: "N/A",
                        reason = leave["reason"]?.toString() ?: "No reason provided",
                        date = leave["date"]?.toString() ?: "",
                        status = leave["status"]?.toString() ?: "Unknown",
                        isHodView = true,
                        onAction = {
                            viewModel.hodAction(id, true) {
                                viewModel.loadHodLeaves()
                            }
                        },
                        actionLabel = "Approve",
                        actionIcon = Icons.Default.Check,
                        isHod = true,
                        onReject = {
                            viewModel.hodAction(id, false) {
                                viewModel.loadHodLeaves()
                            }
                        }
                    )
                }
            }
        }
    }
}
