@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.domain.model.Teacher
import com.auraface.auraface_app.presentation.admin.TeacherViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.auraface.auraface_app.data.network.RetrofitClient

@Composable
fun ManageTeachersScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val teachers by viewModel.teachers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<Teacher?>(null) }
    var assigningTeacher by remember { mutableStateOf<Teacher?>(null) }
    var teacherToDelete by remember { mutableStateOf<Teacher?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTeachers = teachers.filter { teacher ->
        searchQuery.isBlank() || 
        (teacher.full_name?.contains(searchQuery, ignoreCase = true) == true) || 
        teacher.username.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        viewModel.loadTeachers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Teachers", fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, contentDescription = "Add Teacher")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name or username") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && teachers.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredTeachers.isEmpty()) {
                    Text("No teachers found", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTeachers) { teacher ->
                            TeacherItem(
                                teacher = teacher,
                                onEdit = { editingTeacher = it },
                                onDelete = { teacherToDelete = it },
                                onAssign = { assigningTeacher = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TeacherDialog(
            title = "Add Teacher",
            onDismiss = { showAddDialog = false },
            onConfirm = { teacher, file ->
                viewModel.addTeacher(teacher, file)
                showAddDialog = false
            }
        )
    }

    editingTeacher?.let { teacher ->
        TeacherDialog(
            title = "Edit Teacher",
            teacher = teacher,
            onDismiss = { editingTeacher = null },
            onConfirm = { updatedTeacher, file ->
                viewModel.updateTeacher(updatedTeacher, file)
                editingTeacher = null
            }
        )
    }

    assigningTeacher?.let { teacher ->
        AssignmentDialog(
            teacher = teacher,
            onDismiss = { assigningTeacher = null },
            onAssignHod = { dept ->
                viewModel.assignHod(teacher.id, dept)
                assigningTeacher = null
            },
            onAssignClassTeacher = { dept, year, sem, sec ->
                viewModel.assignClassTeacher(teacher.id, dept, year, sem, sec)
                assigningTeacher = null
            }
        )
    }

    teacherToDelete?.let { teacher ->
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            title = { Text("Delete Teacher") },
            text = { Text("Are you sure you want to delete ${teacher.full_name ?: teacher.username}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTeacher(teacher.id)
                        teacherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TeacherItem(
    teacher: Teacher,
    onEdit: (Teacher) -> Unit,
    onDelete: (Teacher) -> Unit,
    onAssign: (Teacher) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Teacher Profile Image in List
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (teacher.profile_image != null) {
                    AsyncImage(
                        model = RetrofitClient.BASE_URL + teacher.profile_image.removePrefix("/"),
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
                Text(teacher.full_name ?: teacher.username, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (teacher.full_name != null) {
                    Text("@${teacher.username}", color = Color.Gray, fontSize = 12.sp)
                }
                teacher.password?.let {
                    Text("Password: $it", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                teacher.qualification?.let {
                    Text("Qual: $it", color = Color.DarkGray, fontSize = 11.sp)
                }
                Text("System Role: Faculty", color = Color.Gray, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAssign(teacher) }) {
                    Icon(Icons.Default.AccountBox, contentDescription = "Assign", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = { onEdit(teacher) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete(teacher) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AssignmentDialog(
    teacher: Teacher,
    onDismiss: () -> Unit,
    onAssignHod: (String) -> Unit,
    onAssignClassTeacher: (String, Int, Int, String) -> Unit
) {
    var department by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("1") }
    var semester by remember { mutableStateOf("1") }
    var section by remember { mutableStateOf("A") }
    var isHodMode by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Role to ${teacher.username}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = isHodMode,
                        onClick = { isHodMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("HOD") }
                    SegmentedButton(
                        selected = !isHodMode,
                        onClick = { isHodMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Class Teacher") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isHodMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = semester,
                            onValueChange = { semester = it },
                            label = { Text("Sem") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = section,
                            onValueChange = { section = it },
                            label = { Text("Sec") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isHodMode) onAssignHod(department)
                else onAssignClassTeacher(department, year.toIntOrNull() ?: 1, semester.toIntOrNull() ?: 1, section)
            }) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TeacherDialog(
    title: String,
    teacher: Teacher? = null,
    onDismiss: () -> Unit,
    onConfirm: (Teacher, java.io.File?) -> Unit
) {
    var username by remember { mutableStateOf(teacher?.username ?: "") }
    var fullName by remember { mutableStateOf(teacher?.full_name ?: "") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(teacher?.email ?: "") }
    var mobile by remember { mutableStateOf(teacher?.mobile ?: "") }
    var address by remember { mutableStateOf(teacher?.address ?: "") }
    var qualification by remember { mutableStateOf(teacher?.qualification ?: "") }
    
    // Image Handling
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                // Image Box
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        coil.compose.AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Profile Image",
                            modifier = Modifier.size(120.dp).clip(androidx.compose.foundation.shape.CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                     } else if (teacher?.profile_image != null) {
                         AsyncImage(
                            model = RetrofitClient.BASE_URL + teacher.profile_image.removePrefix("/"),
                            contentDescription = "Existing Profile Image",
                            modifier = Modifier.size(120.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Default Profile",
                            modifier = Modifier.size(120.dp),
                            tint = Color.Gray
                        )
                    }
                    
                    IconButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-40).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name (e.g. Dr. John Doe)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (teacher == null) "Password" else "New Password (Optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile No") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                 OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Qualification (e.g. PhD, M.Tech)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val file = selectedImageUri?.let { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = java.io.File.createTempFile("profile", ".jpg", context.cacheDir)
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
                
                onConfirm(
                    Teacher(
                        id = teacher?.id ?: 0,
                        username = username,
                        full_name = fullName.ifBlank { null },
                        password = if (password.isEmpty()) null else password,
                        email = email.ifBlank { null },
                        mobile = mobile.ifBlank { null },
                        address = address.ifBlank { null },
                        qualification = qualification.ifBlank { null }
                    ),
                    file
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
