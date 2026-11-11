package com.auraface.auraface_app.presentation.smart

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class StudyTask(
    val id: Int,
    val title: String,
    val subject: String,
    val duration: String,
    val reason: String,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, NORMAL
    var completed: Boolean = false
)

data class StudentStudyPlan(
    val student_name: String,
    val roll_no: String,
    val department: String,
    val semester: Int,
    val section: String,
    val room: String,
    val academic_session: String,
    val overall_performance: String,
    val tasks: List<StudyTask>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartStudyPlannerScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var planData by remember { mutableStateOf<StudentStudyPlan?>(null) }
    var tasksList by remember { mutableStateOf<List<StudyTask>>(emptyList()) }

    fun loadPersonalizedPlan() {
        isLoading = true
        coroutineScope.launch {
            delay(1200) // Realistic AI synthesis
            // Fallback & structured personalized student plan for 7th Sem CSE Section G
            val initialTasks = listOf(
                StudyTask(
                    id = 1,
                    title = "Compiler Design: Practice LR(1) & LALR Parsing Tables",
                    subject = "Compiler Design (CD)",
                    duration = "1 hr 15 mins",
                    reason = "Faculty: Dr. D. Anil Kumar | Current score: 82.0% - Solve Shift/Reduce table conflicts",
                    priority = "HIGH",
                    completed = false
                ),
                StudyTask(
                    id = 2,
                    title = "CNS: Deep-Dive into RSA Encryption & Diffie-Hellman",
                    subject = "Computer Networks and Security (CNS)",
                    duration = "1 hr",
                    reason = "Faculty: Ms. Supriya Satapathy | Target key cryptographic concepts for upcoming test",
                    priority = "HIGH",
                    completed = false
                ),
                StudyTask(
                    id = 3,
                    title = "Data Analytics: K-Means Clustering & PCA Implementation",
                    subject = "Data Analytics (DA)",
                    duration = "45 mins",
                    reason = "Faculty: Dr. Bhabani Sankar Panda | Practical code execution on datasets & inertia analysis",
                    priority = "MEDIUM",
                    completed = false
                ),
                StudyTask(
                    id = 4,
                    title = "Computer Graphics: Bresenham's Line & Cohen-Sutherland Clipping",
                    subject = "Computer Graphics (CG)",
                    duration = "50 mins",
                    reason = "Faculty: Mr. Kedarnath Panda | Current score: 80.5% - Reinforce rasterization math",
                    priority = "HIGH",
                    completed = false
                ),
                StudyTask(
                    id = 5,
                    title = "EED: Business Model Canvas & Break-Even Analysis",
                    subject = "Entrepreneurship (ED/EED)",
                    duration = "35 mins",
                    reason = "Faculty: Dr. A. Chiranjibi Rambabu Achary | Prepare startup MVP & financial overview",
                    priority = "NORMAL",
                    completed = false
                ),
                StudyTask(
                    id = 6,
                    title = "Major Project Phase-I: SRS Documentation & Architecture",
                    subject = "Major Project Phase-I",
                    duration = "2 hrs",
                    reason = "Guide: Dr. D. Anil Kumar | Milestone prep for Wednesday 3-hour lab block",
                    priority = "HIGH",
                    completed = false
                ),
                StudyTask(
                    id = 7,
                    title = "Technical Seminar: PPT Slide Deck & Literature Survey",
                    subject = "Technical Seminar",
                    duration = "40 mins",
                    reason = "Coordinator: Mrs. A. Jyoshana | Prepare Thursday 10:00 AM seminar presentation",
                    priority = "MEDIUM",
                    completed = false
                )
            )

            planData = StudentStudyPlan(
                student_name = "Priyanshu Gupta",
                roll_no = "23CSE513",
                department = "CSE",
                semester = 7,
                section = "G",
                room = "AME-16",
                academic_session = "2026-27",
                overall_performance = "84.5%",
                tasks = initialTasks
            )
            tasksList = initialTasks
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPersonalizedPlan()
    }

    val completedCount = tasksList.count { it.completed }
    val progressFraction = if (tasksList.isNotEmpty()) completedCount.toFloat() / tasksList.size else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AI Study Planner", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Personalized for 7th Sem CSE-G", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadPersonalizedPlan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate Plan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI analyzing attendance & marks...", fontWeight = FontWeight.Medium, color = Color.Gray)
                    Text("Synthesizing personalized study schedule for Section G", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Student Profile Banner
                item {
                    val p = planData
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p?.student_name?.take(2)?.uppercase() ?: "PG",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p?.student_name ?: "Priyanshu Gupta",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Roll: ${p?.roll_no ?: "23CSE513"} • VII Sem CSE - Sec ${p?.section ?: "G"}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Room ${p?.room ?: "AME-16"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Overall progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Goals: $completedCount of ${tasksList.size} Completed",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = progressFraction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White
                            )
                        }
                    }
                }

                // 2. Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Study Modules (${tasksList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "GIETU Session 2026-27",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 3. Task Cards
                items(tasksList, key = { it.id }) { task ->
                    PersonalizedTaskCard(
                        task = task,
                        onToggle = { isChecked ->
                            tasksList = tasksList.map {
                                if (it.id == task.id) it.copy(completed = isChecked) else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalizedTaskCard(
    task: StudyTask,
    onToggle: (Boolean) -> Unit
) {
    val (priorityColor, priorityBg) = when (task.priority) {
        "HIGH" -> Pair(Color(0xFFD32F2F), Color(0xFFFFEBEE))
        "MEDIUM" -> Pair(Color(0xFF1976D2), Color(0xFFE3F2FD))
        else -> Pair(Color(0xFF388E3C), Color(0xFFE8F5E9))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!task.completed) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle(it) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header row with Priority badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.subject,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .background(priorityBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title with strikethrough if done
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (task.completed) Color.Gray else Color.Black,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Suggested Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Suggested duration: ${task.duration}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Reason / Explanation
                Text(
                    text = "Why? ${task.reason}",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
