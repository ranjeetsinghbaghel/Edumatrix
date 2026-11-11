@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.domain.model.Subject
import com.auraface.auraface_app.domain.model.Teacher

@Composable
fun ManageSubjectsScreen(
    navController: NavController,
    viewModel: SubjectViewModel = hiltViewModel()
) {
    val subjects by viewModel.subjects.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSubjects = subjects.filter { subject ->
        searchQuery.isBlank() || subject.name.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Subjects", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && subjects.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredSubjects.isEmpty()) {
                    Text("No subjects found", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredSubjects) { subject ->
                            SubjectItem(
                                subject = subject,
                                teachers = teachers,
                                onEdit = { editingSubject = it },
                                onDelete = { subjectToDelete = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SubjectDialog(
            title = "Add Subject",
            teachers = teachers,
            onDismiss = { showAddDialog = false },
            onConfirm = { 
                viewModel.addSubject(it)
                showAddDialog = false
            }
        )
    }

    editingSubject?.let { subject ->
        SubjectDialog(
            title = "Edit Subject",
            subject = subject,
            teachers = teachers,
            onDismiss = { editingSubject = null },
            onConfirm = {
                viewModel.updateSubject(it)
                editingSubject = null
            }
        )
    }

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject") },
            text = { Text("Are you sure you want to delete ${subject.name} (${subject.id})? This will also remove it from class schedules.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subject.id)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun SubjectItem(
    subject: Subject,
    teachers: List<Teacher>,
    onEdit: (Subject) -> Unit,
    onDelete: (Subject) -> Unit
) {
    val assignedTeacher = teachers.find { it.id == subject.teacher_id }?.let { it.full_name ?: it.username } ?: "No Teacher Assigned"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Code: ${subject.id}", color = Color.Gray, fontSize = 14.sp)
                if (subject.department != null) {
                    Text("Class: ${subject.department} ${subject.year ?: ""} ${if (subject.semester != null) "Sem-${subject.semester}" else ""} ${subject.section ?: ""}", fontSize = 13.sp)
                }
                Text("Instructor: $assignedTeacher", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
            Row {
                IconButton(onClick = { onEdit(subject) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete(subject) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SubjectDialog(
    title: String,
    subject: Subject? = null,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onConfirm: (Subject) -> Unit
) {
    var code by remember { mutableStateOf(subject?.id ?: "") }
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var selectedTeacherId by remember { mutableStateOf(subject?.teacher_id) }
    var dept by remember { mutableStateOf(subject?.department ?: "") }
    var yearStr by remember { mutableStateOf(subject?.year?.toString() ?: "") }
    var semStr by remember { mutableStateOf(subject?.semester?.toString() ?: "") }
    var section by remember { mutableStateOf(subject?.section ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Subject Code (e.g. CS101)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = subject == null // ID is usually immutable
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text("Dept") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Year") },
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = semStr,
                        onValueChange = { semStr = it },
                        label = { Text("Sem") },
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Sec") },
                        modifier = Modifier.weight(0.7f)
                    )
                }
                
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(teachers.find { it.id == selectedTeacherId }?.let { it.full_name ?: it.username } ?: "Select Teacher")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { 
                                selectedTeacherId = null
                                expanded = false 
                            }
                        )
                        teachers.forEach { teacher ->
                            DropdownMenuItem(
                                text = { Text(teacher.full_name ?: teacher.username) },
                                onClick = {
                                    selectedTeacherId = teacher.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    Subject(
                        id = code,
                        name = name,
                        teacher_id = selectedTeacherId,
                        department = dept.ifBlank { null },
                        year = yearStr.toIntOrNull(),
                        semester = semStr.toIntOrNull(),
                        section = section.ifBlank { null }
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
