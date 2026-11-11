@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.auraface.auraface_app.data.network.RetrofitClient
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.auraface.auraface_app.domain.model.Student

@Composable
fun ManageStudentsScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    
    var selectedYear by remember { mutableStateOf("All") }
    var selectedSection by remember { mutableStateOf("All") }
    var yearExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadStudents()
    }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val filteredStudents = students.filter { student ->
        (selectedYear == "All" || student.year.toString() == selectedYear) &&
        (selectedSection == "All" || student.section.contains(selectedSection, ignoreCase = true)) &&
        (searchQuery.isBlank() || student.name.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Students", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStudents() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year Filter
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { yearExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Year: $selectedYear")
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                        listOf("All", "1", "2", "3", "4").forEach { year ->
                            DropdownMenuItem(text = { Text(year) }, onClick = { selectedYear = year; yearExpanded = false })
                        }
                    }
                }
                
                // Section Filter
                OutlinedTextField(
                    value = if (selectedSection == "All") "" else selectedSection,
                    onValueChange = { selectedSection = if (it.isEmpty()) "All" else it },
                    label = { Text("Section") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && students.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredStudents.isEmpty()) {
                    Text("No students found", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredStudents) { student ->
                            StudentItem(
                                student = student,
                                onEdit = { editingStudent = it },
                                onDelete = { studentToDelete = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        StudentDialog(
            title = "Add Student",
            onDismiss = { showAddDialog = false },
            onConfirm = { student, file ->
                viewModel.addStudent(student, file)
                showAddDialog = false
            }
        )
    }

    editingStudent?.let { student ->
        StudentDialog(
            title = "Edit Student",
            student = student,
            onDismiss = { editingStudent = null },
            onConfirm = { updatedStudent, file ->
                viewModel.updateStudent(updatedStudent, file)
                editingStudent = null
            }
        )
    }

    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete ${student.name}? This will also delete their user account and all attendance records.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(student.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun StudentItem(
    student: Student,
    onEdit: (Student) -> Unit,
    onDelete: (Student) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image in List
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (student.profile_image != null) {
                    AsyncImage(
                        model = RetrofitClient.BASE_URL + student.profile_image.removePrefix("/"),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Roll No: ${student.roll_no}", color = Color.Gray, fontSize = 14.sp)
                student.password?.let {
                    Text("Password: $it", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Program: ${student.program ?: "-"} | Dept: ${student.department} | Year: ${student.year} | Sem: ${student.semester ?: "-"} | Sec: ${student.section}", fontSize = 12.sp)
            }
            Row {
                IconButton(onClick = { onEdit(student) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete(student) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun StudentDialog(
    title: String,
    student: Student? = null,
    onDismiss: () -> Unit,
    onConfirm: (Student, java.io.File?) -> Unit
) {
    var name by remember { mutableStateOf(student?.name ?: "") }
    var rollNo by remember { mutableStateOf(student?.roll_no ?: "") }
    var program by remember { mutableStateOf(student?.program ?: "") }
    var department by remember { mutableStateOf(student?.department ?: "") }
    var year by remember { mutableStateOf(student?.year?.toString() ?: "1") }
    var semester by remember { mutableStateOf(student?.semester?.toString() ?: "") }
    var section by remember { mutableStateOf(student?.section ?: "A") }
    
    // New Fields
    var email by remember { mutableStateOf(student?.email ?: "") }
    var mobile by remember { mutableStateOf(student?.mobile ?: "") }
    var guardianName by remember { mutableStateOf(student?.guardian_name ?: "") }
    var guardianEmail by remember { mutableStateOf(student?.guardian_email ?: "") }
    var guardianMobile by remember { mutableStateOf(student?.guardian_mobile ?: "") }

    // Image Picker
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp) // Prevent overflow
            ) {
                // ... (content same as before) ...
                item {
                    // Image Selection
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedImageUri != null) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "New Profile Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (student?.profile_image != null) {
                                    AsyncImage(
                                        model = RetrofitClient.BASE_URL + student.profile_image.removePrefix("/"),
                                        contentDescription = "Current Profile Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            TextButton(onClick = { launcher.launch("image/*") }) {
                                Text(if (selectedImageUri == null && student?.profile_image == null) "Add Photo" else "Change Photo")
                            }
                        }
                    }
                }
                
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = program, onValueChange = { program = it }, label = { Text("Program (e.g., B.Tech, MCA)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth()) }
                
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Sem") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f))
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item { Text("Contact Details", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                
                item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth()) }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item { Text("Guardian Information", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                
                item { OutlinedTextField(value = guardianName, onValueChange = { guardianName = it }, label = { Text("Guardian Name") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = guardianEmail, onValueChange = { guardianEmail = it }, label = { Text("Guardian Email") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = guardianMobile, onValueChange = { guardianMobile = it }, label = { Text("Guardian Mobile") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        selectedImageUri?.let { uri ->
                            getFileFromUri(context, uri)
                        }
                    }
                    onConfirm(
                        Student(
                            id = student?.id ?: 0,
                            name = name,
                            roll_no = rollNo,
                            program = program.ifBlank { null },
                            department = department,
                            year = year.toIntOrNull() ?: 1,
                            semester = semester.toIntOrNull(),
                            section = section,
                            email = email.ifBlank { null },
                            mobile = mobile.ifBlank { null },
                            guardian_name = guardianName.ifBlank { null },
                            guardian_email = guardianEmail.ifBlank { null },
                            guardian_mobile = guardianMobile.ifBlank { null }
                        ),
                        file
                    )
                }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun getFileFromUri(context: android.content.Context, uri: android.net.Uri): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = java.io.File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = java.io.FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
