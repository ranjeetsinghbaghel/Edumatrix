@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.teacher

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.presentation.auth.CameraPermission
import com.auraface.auraface_app.presentation.auth.LocationPermission
import androidx.compose.foundation.lazy.items

@Composable
fun AttendanceScreen(
    navController: NavController,
    subjectId: String,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val result by viewModel.result.observeAsState()
    
    var selectedPeriod by remember { mutableStateOf<String?>(null) }
    var isTakingAttendance by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("Face") } // "Face" or "RollNo"
    
    // New Time State
    var startTime by remember { mutableStateOf<String?>(null) }
    var endTime by remember { mutableStateOf<String?>(null) }
    
    val periods = listOf("Period 1", "Period 2", "Period 3", "Period 4", "Period 5")

    // Time Picker Helper
    fun showTimePicker(initialTime: String?, onTimeSelected: (String) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        android.app.TimePickerDialog(
            context,
            { _, h, m -> 
                val timeStr = String.format("%02d:%02d", h, m)
                onTimeSelected(timeStr)
            },
            hour,
            minute,
            true // 24 hour
        ).show()
    }

    if (!isTakingAttendance) {
        // Selection Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Session Setup") },
                    navigationIcon = {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Select Class Period", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                periods.forEach { period ->
                    OutlinedButton(
                        onClick = { selectedPeriod = period },
                        modifier = Modifier.fillMaxWidth().height(50.dp).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (selectedPeriod == period) 
                             ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                             else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(period)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Time Selection UI
                Text("Class Timing (Required)", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showTimePicker(startTime) { startTime = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(startTime ?: "Start Time")
                    }
                    OutlinedButton(
                        onClick = { showTimePicker(endTime) { endTime = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(endTime ?: "End Time")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Recognition Mode", fontWeight = FontWeight.SemiBold)
                Row(
                   modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                   horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeButton("Face", mode == "Face", Modifier.weight(1f)) { mode = "Face" }
                    ModeButton("Roll No", mode == "RollNo", Modifier.weight(1f)) { mode = "RollNo" }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { isTakingAttendance = true },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    enabled = selectedPeriod != null && startTime != null && endTime != null,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Start ${if(mode == "Face") "Camera" else "Entry"}", fontSize = 16.sp)
                }
            }
        }
    } else {
        if (mode == "Face") {
            // Camera Screen
            LocationPermission {
                CameraPermission {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraScreen(
                            onImageCaptured = { base64Image ->
                                viewModel.markAttendance(
                                    subjectId = subjectId,
                                    period = selectedPeriod!!,
                                    base64Image = base64Image,
                                    startTime = startTime,
                                    endTime = endTime
                                )
                            }
                        )
                        
                        // Overlay info
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Matching for $selectedPeriod",
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        IconButton(
                            onClick = { isTakingAttendance = false },
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }

                    result?.let {
                        // Check if result is LATE
                        if (it.contains("LATE", ignoreCase = true)) {
                             // Show Prompt Dialog (Simplified for now - just Toast)
                             // In real implementation, show AlertDialog for "Enter Late Reason"
                             // Then call markAttendanceManual(reason=...)
                             Toast.makeText(context, "Marked Late: $it", Toast.LENGTH_LONG).show()
                        } else {
                            LaunchedEffect(it) {
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                viewModel.clearResult()
                            }
                        }
                    }
                }
            }
        } else {
            // Manual/Bulk Mode (Student List)
            var yearInput by remember { mutableStateOf("") }
            var sectionInput by remember { mutableStateOf("") }
            val students = viewModel.studentsList

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                if (viewModel.attendanceStep == "Review") "Review Absent List" 
                                else "Mark Present - $selectedPeriod"
                            ) 
                        },
                        navigationIcon = {
                            TextButton(onClick = { isTakingAttendance = false }) {
                                Text("Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
                ) {
                    // FILTER SECTION
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Select Class", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = yearInput,
                                    onValueChange = { yearInput = it },
                                    label = { Text("Year") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = sectionInput,
                                    onValueChange = { sectionInput = it },
                                    label = { Text("Section") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { 
                                    if(yearInput.isNotEmpty() && sectionInput.isNotEmpty()) {
                                        viewModel.getStudentsByClass(yearInput.toIntOrNull() ?: 0, sectionInput)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Get Students")
                            }
                        }
                    }

// STUDENT LIST
                    val step = viewModel.attendanceStep
                    
                    if (students.isNotEmpty()) {
                         if (step == "Review") {
                             Card(
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                 modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                             ) {
                                 Text(
                                     "Review Absent List: Select any student who is actually PRESENT.",
                                     modifier = Modifier.padding(16.dp),
                                     color = MaterialTheme.colorScheme.onErrorContainer
                                 )
                             }
                         }

                         androidx.compose.foundation.lazy.LazyColumn(
                             modifier = Modifier.weight(1f).fillMaxWidth(),
                             verticalArrangement = Arrangement.spacedBy(8.dp)
                         ) {
                             items(students) { student ->
                                 Card(
                                     colors = CardDefaults.cardColors(
                                         containerColor = if (student.isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                     ),
                                     onClick = { viewModel.toggleStudentSelection(student.id) }
                                 ) {
                                     Row(
                                         modifier = Modifier.fillMaxWidth().padding(16.dp),
                                         verticalAlignment = Alignment.CenterVertically,
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Column {
                                             Text(student.name, fontWeight = FontWeight.SemiBold)
                                             Text("Roll: ${student.roll_no}", style = MaterialTheme.typography.bodySmall)
                                         }
                                         Checkbox(
                                             checked = student.isSelected,
                                             onCheckedChange = { viewModel.toggleStudentSelection(student.id) }
                                         )
                                     }
                                 }
                             }
                         }

                         Spacer(modifier = Modifier.height(16.dp))
                         
                         Button(
                             onClick = { viewModel.markBulkAttendance(subjectId, selectedPeriod!!) },
                             modifier = Modifier.fillMaxWidth().height(56.dp),
                             colors = if(step == "Review") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                         ) {
                             if (step == "Marking") {
                                Text("Submit Present (${students.count { it.isSelected }})")
                             } else {
                                Text("Done / Mark Selected Present")
                             }
                         }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No students loaded yet or attendance finished.")
                        }
                    }
                    
                    result?.let {
                        Text(
                            it, 
                            color = if(it.contains("failure") || it.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    viewModel.shareableSummary?.let { summaryText ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Attendance Summary")
                                    putExtra(android.content.Intent.EXTRA_TEXT, summaryText)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share Attendance via"))
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.padding(end = 8.dp))
                            Text("Share Final List")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer) else ButtonDefaults.outlinedButtonColors()
    ) {
        Text(label)
    }
}
