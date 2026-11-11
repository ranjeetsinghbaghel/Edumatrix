@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.placement

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.api.PlacementApi
import com.auraface.auraface_app.data.remote.dto.ResumeProfileDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResumeBuilderViewModel @Inject constructor(
    private val placementApi: PlacementApi
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _profile = MutableStateFlow<ResumeProfileDto?>(null)
    val profile: StateFlow<ResumeProfileDto?> = _profile

    fun loadProfile(onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val res = placementApi.getResume()
                _profile.value = res
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to load resume profile")
            } finally {
                _loading.value = false
            }
        }
    }

    fun saveProfile(profile: ResumeProfileDto, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                placementApi.saveResume(profile)
                _profile.value = profile
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to save profile")
            } finally {
                _loading.value = false
            }
        }
    }
}

@Composable
fun ResumeBuilderScreen(
    navController: NavController,
    viewModel: ResumeBuilderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var selectedTab by remember { mutableStateOf("Profile") }
    val tabs = listOf("Profile", "Education", "Projects", "Skills", "Preview")

    // Contact info
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }
    var github by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }

    // Projects list
    val projectsList = remember { mutableStateListOf<Map<String, String>>() }
    var newProjTitle by remember { mutableStateOf("") }
    var newProjStack by remember { mutableStateOf("") }
    var newProjDesc by remember { mutableStateOf("") }

    // Skills list
    val skillsList = remember { mutableStateListOf<String>() }
    var newSkillName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadProfile { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(profile) {
        if (profile != null) {
            name = profile!!.full_name ?: ""
            email = profile!!.email ?: ""
            phone = profile!!.phone ?: ""
            linkedin = profile!!.linkedin ?: ""
            github = profile!!.github ?: ""
            education = profile!!.education ?: ""
            
            projectsList.clear()
            profile!!.projects.forEach { proj ->
                projectsList.add(proj.mapValues { it.value.toString() })
            }
            
            skillsList.clear()
            skillsList.addAll(profile!!.skills)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Resume CV Builder", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4F46E5)),
                actions = {
                    if (profile != null) {
                        IconButton(onClick = {
                            val updated = ResumeProfileDto(
                                full_name = name,
                                email = email,
                                phone = phone,
                                linkedin = linkedin,
                                github = github,
                                education = education,
                                experience = emptyList(),
                                projects = projectsList.map { it.mapValues { entry -> entry.value as Any } },
                                skills = skillsList.toList()
                            )
                            viewModel.saveProfile(updated, {
                                Toast.makeText(context, "Resume progress saved successfully!", Toast.LENGTH_SHORT).show()
                            }, { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            })
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            if (loading && profile == null) {
                CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.align(Alignment.Center))
            } else if (profile != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom tab layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(Color.White)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEach { tab ->
                            val isSel = tab == selectedTab
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFFEEF2FF) else Color.Transparent,
                                border = if (isSel) BorderStroke(1.dp, Color(0xFF818CF8)) else null,
                                modifier = Modifier.clickable { selectedTab = tab }
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSel) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        when (selectedTab) {
                            "Profile" -> {
                                Text("Contact & Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = github, onValueChange = { github = it }, label = { Text("GitHub Link") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = linkedin, onValueChange = { linkedin = it }, label = { Text("LinkedIn Profile Link") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            }
                            "Education" -> {
                                Text("Academic Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = education,
                                    onValueChange = { education = it },
                                    label = { Text("Education Summary") },
                                    placeholder = { Text("GIET University, B.Tech in CSE...") },
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            "Projects" -> {
                                Text("Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(16.dp))

                                projectsList.forEachIndexed { index, proj ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(proj["title"] ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                                Text(proj["tech_stack"] ?: "", fontSize = 12.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.SemiBold)
                                                Text(proj["description"] ?: "", fontSize = 12.sp, color = Color(0xFF64748B))
                                            }
                                            IconButton(onClick = { projectsList.removeAt(index) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Add Project", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(value = newProjTitle, onValueChange = { newProjTitle = it }, label = { Text("Project Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(value = newProjStack, onValueChange = { newProjStack = it }, label = { Text("Tech Stack") }, placeholder = { Text("e.g. Kotlin, Python") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(value = newProjDesc, onValueChange = { newProjDesc = it }, label = { Text("Project Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (newProjTitle.isNotBlank()) {
                                            projectsList.add(mapOf(
                                                "title" to newProjTitle,
                                                "tech_stack" to newProjStack,
                                                "description" to newProjDesc
                                            ))
                                            newProjTitle = ""
                                            newProjStack = ""
                                            newProjDesc = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Add Project to List", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            "Skills" -> {
                                Text("Technical Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Skills Chips Wrap Flow
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    skillsList.forEach { skill ->
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFFEEF2FF),
                                            border = BorderStroke(1.dp, Color(0xFF818CF8)),
                                            modifier = Modifier.clickable { skillsList.remove(skill) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(skill, color = Color(0xFF4F46E5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF4F46E5))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedTextField(
                                    value = newSkillName,
                                    onValueChange = { newSkillName = it },
                                    label = { Text("Add Skill") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            if (newSkillName.isNotBlank()) {
                                                skillsList.add(newSkillName.trim())
                                                newSkillName = ""
                                            }
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = "Add")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            "Preview" -> {
                                Text("Document Print Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Resume Render Sheet Layout
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp)
                                    ) {
                                        // Header
                                        Text(name.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Email: $email | Phone: $phone", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                        Text("GitHub: $github | LinkedIn: $linkedin", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Education
                                        Text("EDUCATION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(education, fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Skills
                                        Text("TECHNICAL SKILLS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(skillsList.joinToString(", "), fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Projects
                                        Text("ACADEMIC PROJECTS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        projectsList.forEach { proj ->
                                            Text("• ${proj["title"] ?: ""} (${proj["tech_stack"] ?: ""})", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                            Text("  ${proj["description"] ?: ""}", fontSize = 9.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        exportResumeToPdf(
                                            context = context,
                                            name = name,
                                            email = email,
                                            phone = phone,
                                            github = github,
                                            linkedin = linkedin,
                                            education = education,
                                            skills = skillsList,
                                            projects = projectsList
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export PDF Document", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun exportResumeToPdf(
    context: android.content.Context,
    name: String,
    email: String,
    phone: String,
    github: String,
    linkedin: String,
    education: String,
    skills: List<String>,
    projects: List<Map<String, Any>>
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    val fileName = "${name.ifBlank { "Student" }.replace(" ", "_")}_Resume.pdf"

    // Header Background Accent
    paint.color = android.graphics.Color.rgb(30, 58, 138)
    canvas.drawRect(0f, 0f, 595f, 100f, paint)

    // Name
    paint.textSize = 22f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.WHITE
    canvas.drawText(name.ifBlank { "STUDENT NAME" }.uppercase(), 40f, 50f, paint)

    // Contact Info in Header
    paint.textSize = 9.5f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.rgb(224, 231, 255)
    canvas.drawText("Email: $email   |   Phone: $phone", 40f, 70f, paint)
    canvas.drawText("GitHub: $github   |   LinkedIn: $linkedin", 40f, 85f, paint)

    // Section 1: Education
    var yPos = 130f
    paint.color = android.graphics.Color.rgb(30, 58, 138)
    paint.textSize = 12f
    paint.isFakeBoldText = true
    canvas.drawText("EDUCATION & ACADEMIC BACKGROUND", 40f, yPos, paint)

    paint.color = android.graphics.Color.rgb(59, 130, 246)
    canvas.drawLine(40f, yPos + 4f, 555f, yPos + 4f, paint)

    yPos += 22f
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 10f
    paint.isFakeBoldText = false
    val eduText = if (education.isNotBlank()) education else "B.Tech in Computer Science & Engineering (Batch 2023 - 2027)\nGIET University, Gunupur"
    for (line in eduText.split("\n")) {
        canvas.drawText(line, 40f, yPos, paint)
        yPos += 14f
    }

    // Section 2: Technical Skills
    yPos += 10f
    paint.color = android.graphics.Color.rgb(30, 58, 138)
    paint.textSize = 12f
    paint.isFakeBoldText = true
    canvas.drawText("TECHNICAL SKILLS & CORE COMPETENCIES", 40f, yPos, paint)

    paint.color = android.graphics.Color.rgb(59, 130, 246)
    canvas.drawLine(40f, yPos + 4f, 555f, yPos + 4f, paint)

    yPos += 22f
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 10f
    paint.isFakeBoldText = false
    val skillsDisplay = if (skills.isNotEmpty()) skills.joinToString("  •  ") else "Kotlin, Java, Python, Android SDK, Jetpack Compose, REST APIs, Git"
    canvas.drawText(skillsDisplay, 40f, yPos, paint)

    // Section 3: Academic & Capstone Projects
    yPos += 25f
    paint.color = android.graphics.Color.rgb(30, 58, 138)
    paint.textSize = 12f
    paint.isFakeBoldText = true
    canvas.drawText("KEY PROJECTS & ACHIEVEMENTS", 40f, yPos, paint)

    paint.color = android.graphics.Color.rgb(59, 130, 246)
    canvas.drawLine(40f, yPos + 4f, 555f, yPos + 4f, paint)

    yPos += 22f
    val displayProjects = if (projects.isNotEmpty()) projects else listOf(
        mapOf("title" to "EduMatrix Smart Campus App", "tech_stack" to "Kotlin, Jetpack Compose, FastAPI", "description" to "End-to-end academic management system with facial recognition biometrics and real-time attendance."),
        mapOf("title" to "AI Mock Interviewer Platform", "tech_stack" to "Python, LLM, REST API", "description" to "AI-driven virtual interview engine evaluating technical aptitude with real-time scoring.")
    )

    displayProjects.forEach { proj ->
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 10f
        paint.isFakeBoldText = true
        val title = proj["title"] as? String ?: "Project"
        val tech = proj["tech_stack"] as? String ?: ""
        canvas.drawText("• $title" + if (tech.isNotBlank()) "  [$tech]" else "", 40f, yPos, paint)
        yPos += 14f

        paint.textSize = 9f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.DKGRAY
        val desc = proj["description"] as? String ?: ""
        if (desc.isNotBlank()) {
            canvas.drawText("   $desc", 40f, yPos, paint)
            yPos += 18f
        } else {
            yPos += 6f
        }
    }

    pdfDocument.finishPage(page)

    try {
        // 1. Save to App Cache for immediate FileProvider viewing
        val cacheFile = File(context.cacheDir, fileName)
        val cacheOut = FileOutputStream(cacheFile)
        pdfDocument.writeTo(cacheOut)
        cacheOut.flush()
        cacheOut.close()

        // 2. Save directly to public device Downloads folder
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                val outStream = context.contentResolver.openOutputStream(uri)
                if (outStream != null) {
                    val inStream = FileInputStream(cacheFile)
                    inStream.copyTo(outStream)
                    inStream.close()
                    outStream.flush()
                    outStream.close()
                }
            }
        } else {
            val publicFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            val publicOut = FileOutputStream(publicFile)
            val inStream = FileInputStream(cacheFile)
            inStream.copyTo(publicOut)
            inStream.close()
            publicOut.flush()
            publicOut.close()
        }

        Toast.makeText(context, "✅ Resume saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

        // 3. Automatically launch PDF Viewer intent
        val fileUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            cacheFile
        )
        val openIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = android.content.Intent.createChooser(openIntent, "Open Resume PDF").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)

    } catch (e: Exception) {
        Toast.makeText(context, "Failed to download PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    } finally {
        pdfDocument.close()
    }
}
