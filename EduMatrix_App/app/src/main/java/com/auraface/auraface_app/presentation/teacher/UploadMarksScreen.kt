package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.AssignedSubject
import com.auraface.auraface_app.data.network.model.SubjectDTO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMarksScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val dashboardData = viewModel.dashboardData
    val assignedSubjects = dashboardData?.assignedSubjects ?: emptyList()

    // Toggle: My Classes vs All Classes
    var showAllClasses by remember { mutableStateOf(false) }

    // 1. My Classes Logic
    val classes = remember(assignedSubjects) {
        assignedSubjects
            .filter { it.department != null && it.year != null && it.section != null }
            .map { Triple(it.department!!, it.year!!, it.section!!) }
            .distinct()
    }
    
    var selectedClass by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
    var expandedClass by remember { mutableStateOf(false) }

    // 2. All Classes Logic
    var manualDept by remember { mutableStateOf("") }
    var manualYear by remember { mutableStateOf("") }
    var manualSection by remember { mutableStateOf("") }
    
    // Subject Logic
    // Store selected subject as ID and Name (independent of source)
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedSubjectName by remember { mutableStateOf<String?>(null) }
    var expandedSubject by remember { mutableStateOf(false) }

    // Available subjects for dropdown
    var availableSubjects by remember { mutableStateOf<List<SubjectDTO>>(emptyList()) }

    // When mode changes or inputs change, update available subjects
    LaunchedEffect(showAllClasses, selectedClass, viewModel.subjectsForSelection) {
        if (!showAllClasses) {
            // Filter assigned subjects
            if (selectedClass != null) {
                availableSubjects = assignedSubjects
                    .filter { 
                        it.department == selectedClass!!.first && 
                        it.year == selectedClass!!.second && 
                        it.section == selectedClass!!.third 
                    }
                    .map { SubjectDTO(it.id, it.name, it.department, it.year, it.semester, it.section, 3) }
            } else {
                availableSubjects = emptyList()
            }
        } else {
            // Use fetched subjects
            availableSubjects = viewModel.subjectsForSelection
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Bulk Upload", "View History")

    // Upload Form State
    var assessmentType by remember { mutableStateOf("Midterm") }
    var expandedType by remember { mutableStateOf(false) }
    val assessmentTypes = listOf("Midterm", "Final", "Assignment", "Quiz", "Lab", "Class Test")

    var totalMarks by remember { mutableStateOf("100") }
    
    // Students List & Marks State
    val studentsList = viewModel.studentsForMarking
    val marksMap = remember { mutableStateMapOf<Int, String>() }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSubjectId, selectedTab) {
        if (selectedTab == 1 && selectedSubjectId != null) {
            viewModel.loadSubjectMarks(selectedSubjectId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Marks") },
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
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                
                // Mode Toggle
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("My Classes", style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = showAllClasses,
                        onCheckedChange = { 
                            showAllClasses = it 
                            selectedSubjectId = null
                            selectedSubjectName = null
                            selectedClass = null
                            // Reset manual fetching?
                            if (it) viewModel.getSubjects(null, null, null) // Clear or fetch all?
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text("All Classes", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!showAllClasses) {
                    // My Classes Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedClass,
                        onExpandedChange = { expandedClass = !expandedClass }
                    ) {
                         OutlinedTextField(
                            value = if (selectedClass != null) "${selectedClass!!.first} - Year ${selectedClass!!.second} - Sec ${selectedClass!!.third}" else "Select Class",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedClass,
                            onDismissRequest = { expandedClass = false }
                        ) {
                            classes.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text("${cls.first} - Year ${cls.second} - Sec ${cls.third}") },
                                    onClick = {
                                        selectedClass = cls
                                        selectedSubjectId = null
                                        selectedSubjectName = null
                                        expandedClass = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Manual Input for All Classes
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualDept,
                                onValueChange = { manualDept = it },
                                label = { Text("Dept (e.g. CSE)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = manualYear,
                                onValueChange = { manualYear = it },
                                label = { Text("Year") },
                                modifier = Modifier.weight(0.7f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = manualSection,
                                onValueChange = { manualSection = it },
                                label = { Text("Sec") },
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                        Button(
                            onClick = {
                                if (manualDept.isNotBlank() && manualYear.isNotBlank() && manualSection.isNotBlank()) {
                                    viewModel.getSubjects(manualDept, manualYear.toIntOrNull(), manualSection)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Load Subjects")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedSubject,
                    onExpandedChange = { expandedSubject = !expandedSubject }
                ) {
                    OutlinedTextField(
                        value = selectedSubjectName ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubject,
                        onDismissRequest = { expandedSubject = false }
                    ) {
                        availableSubjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject.name) },
                                onClick = {
                                    selectedSubjectId = subject.id
                                    selectedSubjectName = subject.name
                                    expandedSubject = false
                                }
                            )
                        }
                        if (availableSubjects.isEmpty()) {
                            DropdownMenuItem(text = { Text("No subjects available") }, onClick = { expandedSubject = false })
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Bulk Upload Form
                Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Exam Type
                        ExposedDropdownMenuBox(
                            expanded = expandedType,
                            onExpandedChange = { expandedType = !expandedType },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = assessmentType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Exam Type") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedType,
                                onDismissRequest = { expandedType = false }
                            ) {
                                assessmentTypes.forEach { type ->
                                    DropMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            assessmentType = type
                                            expandedType = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = totalMarks,
                            onValueChange = { totalMarks = it },
                            label = { Text("Total Marks") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            if (!showAllClasses && selectedClass != null) {
                                viewModel.getStudentsForClass(
                                    selectedClass!!.first, 
                                    selectedClass!!.second, 
                                    selectedClass!!.third
                                )
                            } else if (showAllClasses && manualDept.isNotBlank()) {
                                viewModel.getStudentsForClass(
                                    manualDept,
                                    manualYear.toIntOrNull() ?: 0,
                                    manualSection
                                )
                            }
                            // Clear previous marks map
                            marksMap.clear()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        enabled = (!showAllClasses && selectedClass != null) || (showAllClasses && manualDept.isNotBlank())
                    ) {
                        Text("Fetch / Refresh Students List")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(studentsList) { student ->
                             Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(student.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(student.roll_no ?: "No Roll No", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                OutlinedTextField(
                                    value = marksMap[student.id] ?: "",
                                    onValueChange = { marksMap[student.id] = it },
                                    label = { Text("Score") },
                                    modifier = Modifier.width(100.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                    
                    if (studentsList.isNotEmpty()) {
                        Button(
                            onClick = {
                                if (selectedSubjectId != null) {
                                    isLoading = true
                                    val marksPayload = marksMap.mapNotNull { (id, scoreStr) ->
                                        val s = scoreStr.toFloatOrNull()
                                        if (s != null) com.auraface.auraface_app.data.network.model.QuickMarkCreate(id, s) else null
                                    }
                                    
                                    viewModel.addMarksBulk(
                                        subjectId = selectedSubjectId!!,
                                        assessmentType = assessmentType,
                                        totalMarks = totalMarks.toFloatOrNull() ?: 100f,
                                        marks = marksPayload,
                                        onSuccess = {
                                            isLoading = false
                                            successMessage = "Marks saved for ${marksPayload.size} students!"
                                            errorMessage = null
                                            marksMap.clear()
                                        },
                                        onError = {
                                            isLoading = false
                                            errorMessage = it
                                        }
                                    )
                                } else {
                                    errorMessage = "Please select a subject"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text("Save All Marks")
                        }
                    }
                }
                
                if (isLoading) CircularProgressIndicator()
                if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                if (successMessage != null) Text(successMessage!!, color = MaterialTheme.colorScheme.primary)

            } else {
                // View History Tab
                 if (selectedSubjectId == null) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                         Text("Please select a Class and Subject above to view history.")
                    }
                } else {
                    val marksList = viewModel.subjectMarks
                    LazyColumn(
                         contentPadding = PaddingValues(16.dp),
                         verticalArrangement = Arrangement.spacedBy(8.dp),
                         modifier = Modifier.weight(1f)
                    ) {
                        items(marksList) { mark ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                ListItem(
                                    headlineContent = { Text(mark.student_name ?: "Roll No: ${mark.roll_no ?: "N/A"}") },
                                    supportingContent = { Text("${mark.assessment_type} | Total: ${mark.total_marks}") },
                                    trailingContent = { 
                                        Text(
                                            "${mark.score}", 
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        ) 
                                    }
                                )
                            }
                        }
                        if (marksList.isEmpty()) {
                            item {
                                Text("No marks uploaded yet for this subject.", modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropMenuItem(text: @Composable () -> Unit, onClick: () -> Unit) {
    DropdownMenuItem(text = text, onClick = onClick)
}
