package com.auraface.auraface_app.presentation.teacher

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.MarksResponse
import com.auraface.auraface_app.data.network.model.StudentBasic
import com.auraface.auraface_app.data.network.model.ProctorMeeting
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyClassScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val dashboardData = viewModel.dashboardData
    val isClassTeacher = dashboardData?.isClassTeacher == true
    val myClass = dashboardData?.myClass

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Students", "Proctor Meetings")

    val students = viewModel.myClassStudents
    val meetings = viewModel.proctorMeetings
    
    // View Results Dialog
    var showResultsDialog by remember { mutableStateOf(false) }
    var selectedStudentResults by remember { mutableStateOf<List<MarksResponse>>(emptyList()) }
    var selectedStudentName by remember { mutableStateOf("") }
    
    // Add Proctor Meeting Dialog
    var showAddMeetingDialog by remember { mutableStateOf(false) }

    // Send Message Dialog
    var showMessageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isClassTeacher, myClass) {
        if (isClassTeacher && myClass != null) {
            viewModel.getMyClassStudents(myClass.department, myClass.year, myClass.section)
            viewModel.getProctorMeetings()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (myClass != null) 
                        Text("My Class - ${myClass.department} ${myClass.year} ${myClass.section}") 
                    else 
                        Text("My Class")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isClassTeacher) {
                         IconButton(onClick = { showMessageDialog = true }) {
                            Icon(Icons.Default.Email, contentDescription = "Send Message")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isClassTeacher && selectedTab == 1) {
                FloatingActionButton(onClick = { showAddMeetingDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Meeting")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
             if (!isClassTeacher || myClass == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("You are not assigned as a Class Teacher.")
                }
            } else {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedTab == 0) {
                    // Students Marks View
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(students) { student ->
                            Card(
                                onClick = {
                                    selectedStudentName = student.name
                                    viewModel.getStudentResults(student.id) { results ->
                                        selectedStudentResults = results
                                        showResultsDialog = true
                                    }
                                }
                            ) {
                                ListItem(
                                    headlineContent = { Text(student.name, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
                                    supportingContent = {
                                        Column {
                                            Text("Roll No: ${student.roll_no ?: "N/A"}")
                                            if (!student.guardian_name.isNullOrBlank() || !student.guardian_mobile.isNullOrBlank()) {
                                                Text(
                                                    "Guardian: ${student.guardian_name ?: "Parent"} (${student.guardian_mobile ?: "No phone"})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            if (!student.guardian_mobile.isNullOrBlank()) {
                                                IconButton(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.guardian_mobile}"))
                                                        context.startActivity(intent)
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Phone,
                                                        contentDescription = "Call Guardian",
                                                        tint = Color(0xFF16A34A),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    selectedStudentName = student.name
                                                    viewModel.getStudentResults(student.id) { results ->
                                                        selectedStudentResults = results
                                                        showResultsDialog = true
                                                    }
                                                }
                                            ) {
                                                Text("Marks", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Proctor Meetings List
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(meetings) { meeting ->
                            Card {
                                ListItem(
                                    headlineContent = { Text(meeting.student_name ?: "Student #${meeting.student_id}") },
                                    supportingContent = { 
                                        Column {
                                            Text("Date: ${meeting.date}\nRemarks: ${meeting.remarks}") 
                                            if (!meeting.student_response.isNullOrBlank()) {
                                                Text("Student Note: ${meeting.student_response}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        if (meeting.action_taken != null) Text("Action: ${meeting.action_taken}", style = MaterialTheme.typography.bodySmall)
                                    }
                                )
                            }
                        }
                        if (meetings.isEmpty()) {
                            item { Text("No meetings recorded.", modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
             }
        }
    }

    if (showResultsDialog) {
        AlertDialog(
            onDismissRequest = { showResultsDialog = false },
            title = { Text("Results: $selectedStudentName") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(selectedStudentResults) { result ->
                        ListItem(
                            headlineContent = { Text(result.subject_name ?: result.subject_id) },
                            supportingContent = { Text("${result.assessment_type} | Total: ${result.total_marks}") },
                            trailingContent = { Text("${result.score}", style = MaterialTheme.typography.titleMedium) }
                        )
                        HorizontalDivider()
                    }
                    if (selectedStudentResults.isEmpty()) {
                        item { Text("No marks found.") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResultsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddMeetingDialog) {
        AddProctorMeetingDialog(
            students = students,
            onDismiss = { showAddMeetingDialog = false },
            onConfirm = { studentId, remarks, action ->
                viewModel.addProctorMeeting(studentId, LocalDate.now().toString(), remarks, action) {
                    showAddMeetingDialog = false
                    viewModel.getProctorMeetings() 
                }
            }
        )
    }

    if (showMessageDialog) {
        SendSectionMessageDialog(
            students = students,
            onDismiss = { showMessageDialog = false },
            onConfirm = { msg, studentId ->
                viewModel.sendSectionMessage(msg, studentId) {
                    showMessageDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProctorMeetingDialog(
    students: List<StudentBasic>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String?) -> Unit
) {
    var selectedStudent by remember { mutableStateOf<StudentBasic?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var remarks by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Proctor Meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.name ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Student") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        students.forEach { student ->
                            DropdownMenuItem(
                                text = { Text(student.name) },
                                onClick = {
                                    selectedStudent = student
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("Action Taken (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStudent != null && remarks.isNotBlank()) {
                        onConfirm(selectedStudent!!.id, remarks, action.ifBlank { null })
                    }
                },
                enabled = selectedStudent != null && remarks.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendSectionMessageDialog(
    students: List<StudentBasic>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var sendToAll by remember { mutableStateOf(true) }
    var selectedStudent by remember { mutableStateOf<StudentBasic?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Message to Class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = sendToAll, onCheckedChange = { sendToAll = it })
                    Text("Send to All Students")
                }
                
                if (!sendToAll) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "Select Student",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recipient") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            students.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text(student.name) },
                                    onClick = {
                                        selectedStudent = student
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.isNotBlank() && (sendToAll || selectedStudent != null)) {
                        onConfirm(message, if (sendToAll) null else selectedStudent!!.id)
                    }
                },
                enabled = message.isNotBlank() && (sendToAll || selectedStudent != null)
            ) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
