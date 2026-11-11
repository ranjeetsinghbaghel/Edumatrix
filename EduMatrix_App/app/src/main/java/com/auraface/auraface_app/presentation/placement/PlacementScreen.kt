package com.auraface.auraface_app.presentation.placement

import com.auraface.auraface_app.core.navigation.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auraface.auraface_app.data.remote.dto.StudentReadinessDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementReadinessScreen(
    navController: androidx.navigation.NavController,
    viewModel: PlacementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Dialog States
    var showSkillDialog by remember { mutableStateOf(false) }
    var editSkillData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementSkillDto?>(null) }
    
    var showProjectDialog by remember { mutableStateOf(false) }
    var editProjectData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementProjectDto?>(null) }
    
    var showCertDialog by remember { mutableStateOf(false) }
    var editCertData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementCertificationDto?>(null) }
    
    var showInternDialog by remember { mutableStateOf(false) }
    var editInternData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto?>(null) }
    
    var showEventDialog by remember { mutableStateOf(false) }
    var editEventData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementEventDto?>(null) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Placement Readiness") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            ) 
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val s = state) {
                is PlacementState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is PlacementState.Error -> Text("Error: ${s.message}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                is PlacementState.Success -> {
                    ReadinessContent(
                        data = s.data,
                        onAddSkill = { showSkillDialog = true },
                        onAddProject = { showProjectDialog = true },
                        onAddCert = { showCertDialog = true },
                        onAddIntern = { showInternDialog = true },
                        onAddEvent = { showEventDialog = true },
                        onDeleteSkill = { viewModel.deleteSkill(it) },
                        onDeleteProject = { viewModel.deleteProject(it) },
                        onDeleteCert = { viewModel.deleteCertification(it) },
                        onDeleteIntern = { viewModel.deleteInternship(it) },
                        onDeleteEvent = { viewModel.deleteEvent(it) },
                        onEditSkill = { editSkillData = it },
                        onEditProject = { editProjectData = it },
                        onEditCert = { editCertData = it },
                        onEditIntern = { editInternData = it },
                        onEditEvent = { editEventData = it },
                        onNavigateToInterview = { navController.navigate(Screen.AIInterview.route) },
                        onNavigateToResume = { navController.navigate(Screen.ResumeBuilder.route) }
                    )
                }
            }
        }
        
        if (showSkillDialog || editSkillData != null) {
            AddSkillDialog(
                existing = editSkillData,
                onDismiss = { showSkillDialog = false; editSkillData = null }
            ) { name, level, file ->
                if (editSkillData != null) {
                    viewModel.editSkill(editSkillData!!.id, name, level, file)
                } else {
                    viewModel.addSkill(name, level, file)
                }
                showSkillDialog = false
                editSkillData = null
            }
        }
        
        if (showProjectDialog || editProjectData != null) {
            AddProjectDialog(
                existing = editProjectData,
                onDismiss = { showProjectDialog = false; editProjectData = null }
            ) { title, desc, stack, status, file ->
                if (editProjectData != null) {
                    viewModel.editProject(editProjectData!!.id, title, desc, stack, status, file)
                } else {
                    viewModel.addProject(title, desc, stack, status, file)
                }
                showProjectDialog = false
                editProjectData = null
            }
        }

        if (showCertDialog || editCertData != null) {
            AddCertDialog(
                existing = editCertData,
                onDismiss = { showCertDialog = false; editCertData = null }
            ) { name, issuer, file ->
                if (editCertData != null) {
                    viewModel.editCertification(editCertData!!.id, name, issuer, file)
                } else {
                    viewModel.addCertification(name, issuer, file)
                }
                showCertDialog = false
                editCertData = null
            }
        }
        
        if (showInternDialog || editInternData != null) {
             AddInternDialog(
                existing = editInternData,
                onDismiss = { showInternDialog = false; editInternData = null }
            ) { company, role, startDate, endDate, file ->
                if (editInternData != null) {
                    viewModel.editInternship(editInternData!!.id, company, role, startDate, endDate, file)
                } else {
                    viewModel.addInternship(company, role, startDate, endDate, file)
                }
                showInternDialog = false
                editInternData = null
             }
        }
        
        if (showEventDialog || editEventData != null) {
            AddEventDialog(
                existing = editEventData,
                onDismiss = { showEventDialog = false; editEventData = null }
            ) { name, type, date, file ->
                if (editEventData != null) {
                    viewModel.editEvent(editEventData!!.id, name, type, date, file)
                } else {
                    viewModel.addEvent(name, type, date, file)
                }
                showEventDialog = false
                editEventData = null
            }
        }
    }
}

@Composable
fun ReadinessContent(
    data: StudentReadinessDto,
    onAddSkill: () -> Unit,
    onAddProject: () -> Unit,
    onAddCert: () -> Unit,
    onAddIntern: () -> Unit,
    onAddEvent: () -> Unit,
    onDeleteSkill: (Int) -> Unit,
    onDeleteProject: (Int) -> Unit,
    onDeleteCert: (Int) -> Unit,
    onDeleteIntern: (Int) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onEditSkill: (com.auraface.auraface_app.data.remote.dto.PlacementSkillDto) -> Unit,
    onEditProject: (com.auraface.auraface_app.data.remote.dto.PlacementProjectDto) -> Unit,
    onEditCert: (com.auraface.auraface_app.data.remote.dto.PlacementCertificationDto) -> Unit,
    onEditIntern: (com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto) -> Unit,
    onEditEvent: (com.auraface.auraface_app.data.remote.dto.PlacementEventDto) -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToResume: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Overall Readiness",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${data.total_score}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = { data.total_score / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF00E676), // vibrant green
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        
                        if (data.missing_elements.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Areas to Improve:", 
                                        style = MaterialTheme.typography.labelLarge, 
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    data.missing_elements.take(3).forEach {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                it, 
                                                style = MaterialTheme.typography.bodyMedium, 
                                                color = Color.White.copy(alpha = 0.95f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Build Resume CV", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = onNavigateToInterview,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Interview Prep", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }

        // Skills Section
        item { SectionHeader("Technical Skills", onAddSkill, Icons.Default.Code) }
        if (data.skills.isEmpty()) item { EmptyState("No skills added yet.", Icons.Default.Build) }
        items(data.skills) { skill ->
            PremiumListItem(
                title = skill.skill_name,
                subtitle = "Proficiency: ${skill.proficiency}",
                iconText = skill.skill_name.take(1).uppercase(),
                gradient = listOf(Color(0xFFFDC830), Color(0xFFF37335)),
                onDelete = { onDeleteSkill(skill.id) },
                onEdit = { onEditSkill(skill) }
            )
        }
        
        // Projects Section
        item { SectionHeader("Projects", onAddProject, Icons.Default.Star) }
        if (data.projects.isEmpty()) item { EmptyState("No projects added yet.", Icons.Default.Star) }
        items(data.projects) { proj ->
            PremiumListItem(
                title = proj.title,
                subtitle = "${proj.tech_stack}",
                iconText = proj.title.take(1).uppercase(),
                gradient = listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),
                trailingText = proj.project_status,
                onDelete = { onDeleteProject(proj.id) },
                onEdit = { onEditProject(proj) }
            )
        }

        // Certifications Section
        item { SectionHeader("Certifications", onAddCert, Icons.Default.Star) }
        if (data.certifications.isEmpty()) item { EmptyState("No certifications added yet.", Icons.Default.Star) }
        items(data.certifications) { cert ->
             PremiumListItem(
                title = cert.name,
                subtitle = cert.issuing_org,
                iconText = cert.name.take(1).uppercase(),
                gradient = listOf(Color(0xFF43CEA2), Color(0xFF185A9D)),
                trailingText = cert.verification_status,
                onDelete = { onDeleteCert(cert.id) },
                onEdit = { onEditCert(cert) }
            )
        }
        
        // Internships Section
        item { SectionHeader("Internships", onAddIntern, Icons.Default.Work) }
        if (data.internships.isEmpty()) item { EmptyState("No internships added yet.", Icons.Default.Work) }
        items(data.internships) { intern ->
             PremiumListItem(
                title = intern.role,
                subtitle = intern.company_name,
                iconText = intern.company_name.take(1).uppercase(),
                gradient = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
                trailingText = intern.verification_status,
                onDelete = { onDeleteIntern(intern.id) },
                onEdit = { onEditIntern(intern) }
            )
        }

        // Events Section
        item { SectionHeader("Events & Hackathons", onAddEvent, Icons.Default.Event) }
        if (data.events.isEmpty()) item { EmptyState("No events added yet.", Icons.Default.Event) }
        items(data.events) { event ->
             PremiumListItem(
                title = event.event_name,
                subtitle = event.event_type,
                iconText = event.event_name.take(1).uppercase(),
                gradient = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                trailingText = event.verification_status,
                onDelete = { onDeleteEvent(event.id) },
                onEdit = { onEditEvent(event) }
            )
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun PremiumListItem(
    title: String,
    subtitle: String,
    iconText: String,
    gradient: List<Color>,
    trailingText: String? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = gradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (trailingText != null) {
                val isPending = trailingText.contains("Pending", ignoreCase = true) || trailingText.contains("Ongoing", ignoreCase = true)
                val isApproved = trailingText.contains("Approved", ignoreCase = true) || trailingText.contains("Verified", ignoreCase = true) || trailingText.contains("Completed", ignoreCase = true)
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPending) Color(0xFFFFF3E0) 
                            else if (isApproved) Color(0xFFE8F5E9)
                            else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isPending) Color(0xFFEF6C00) 
                                else if (isApproved) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (onEdit != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onAdd: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(
            onClick = onAdd,
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add $title", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun EmptyState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text, 
            style = MaterialTheme.typography.bodyMedium, 
            color = Color.Gray
        )
    }
}

// --- Dialogs ---

@Composable
fun AddSkillDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementSkillDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, java.io.File?) -> Unit) {
    var name by remember { mutableStateOf(existing?.skill_name ?: "") }
    var level by remember { mutableStateOf(existing?.proficiency ?: "Beginner") }
    var selectedFile by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = context.contentResolver
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedFile = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Skill" else "Add Skill") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Skill Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("Proficiency") })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch("*/*") }) {
                    Text(if (selectedFile != null) "File Selected" else "Attach Proof (Optional)")
                }
                if (selectedFile != null) {
                    Text(selectedFile!!.lastPathSegment ?: "File", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if(name.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(name, level, file)
                }
            }) { Text(if (existing != null) "Update" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddProjectDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementProjectDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, String, java.io.File?) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var stack by remember { mutableStateOf(existing?.tech_stack ?: "") }
    var status by remember { mutableStateOf(existing?.project_status ?: "ONGOING") }
    var selectedFile by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = context.contentResolver
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedFile = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Project" else "Add Project") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = stack, onValueChange = { stack = it }, label = { Text("Tech Stack (e.g. Kotlin, Python)") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (e.g. COMPLETED, ONGOING)") })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch("*/*") }) {
                    Text(if (selectedFile != null) "File Selected" else "Attach Proof (Optional)")
                }
                if (selectedFile != null) {
                    Text(selectedFile!!.lastPathSegment ?: "File", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if(title.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(title, desc, stack, status, file)
                }
            }) { Text(if (existing != null) "Update" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCertDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementCertificationDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, java.io.File?) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var issuer by remember { mutableStateOf(existing?.issuing_org ?: "") }
    var selectedFile by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = context.contentResolver
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedFile = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Certification" else "Add Certification") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Certificate Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = issuer, onValueChange = { issuer = it }, label = { Text("Issuing Organization") })
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(onClick = { launcher.launch("*/*") }) {
                    Text(if (selectedFile != null) "File Selected" else "Attach Certificate (PDF/Image)")
                }
                if (selectedFile != null) {
                    Text(selectedFile!!.lastPathSegment ?: "File", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if(name.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(name, issuer, file)
                }
            }) { Text(if (existing != null) "Update" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddInternDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, String, java.io.File?) -> Unit) {
    var company by remember { mutableStateOf(existing?.company_name ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: "") }
    var startDate by remember { mutableStateOf(if (existing?.start_date != null) existing.start_date.take(10) else "2024-01-01") } 
    var endDate by remember { mutableStateOf(if (existing?.end_date != null) existing.end_date.take(10) else "") } 
    var selectedFile by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = context.contentResolver
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedFile = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Internship" else "Add Internship") },
        text = {
            Column {
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD)") }, placeholder = { Text("2024-01-01") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date (Optional)") }, placeholder = { Text("2024-06-01") })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch("*/*") }) {
                    Text(if (selectedFile != null) "File Selected" else "Attach Proof (Optional)")
                }
                if (selectedFile != null) {
                    Text(selectedFile!!.lastPathSegment ?: "File", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if(company.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(company, role, startDate, endDate, file)
                }
            }) { Text(if (existing != null) "Update" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddEventDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementEventDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, java.io.File?) -> Unit) {
    var name by remember { mutableStateOf(existing?.event_name ?: "") }
    var type by remember { mutableStateOf(existing?.event_type ?: "Hackathon") }
    var date by remember { mutableStateOf(if(existing?.date != null) existing.date.take(10) else "2024-01-01") } 
    var selectedFile by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = context.contentResolver
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedFile = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Event" else "Add Event") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (e.g. Hackathon, Workshop)") })
                 Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch("*/*") }) {
                    Text(if (selectedFile != null) "File Selected" else "Attach Proof (Optional)")
                }
                if (selectedFile != null) {
                    Text(selectedFile!!.lastPathSegment ?: "File", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if(name.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(name, type, date, file)
                }
            }) { Text(if (existing != null) "Update" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun generateTempUploadFile(context: android.content.Context, uri: android.net.Uri?): java.io.File? {
    if (uri == null) return null
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val mimeType = contentResolver.getType(uri)
        val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val suffix = if (extension != null) ".$extension" else ".tmp"
        val tempFile = java.io.File.createTempFile("upload_", suffix, context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream?.copyTo(output)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
