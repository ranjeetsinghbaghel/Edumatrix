@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.core.navigation.Screen

@Composable
fun TeacherDashboardScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val data = viewModel.dashboardData
    var showMessageClassDialog by remember { mutableStateOf(false) }
    var selectedSubjectForMessage by remember { mutableStateOf<com.auraface.auraface_app.data.network.model.AssignedSubject?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Faculty Dashboard", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0EA5E9) // Light Blue Header
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // Gradient Header with Summary Stats
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0EA5E9), Color(0xFF2563EB))
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TeacherStatCard(
                        label = "Assigned Subjects",
                        value = data?.totalAssignedSubjects?.toString() ?: "-",
                        icon = Icons.AutoMirrored.Filled.List,
                        modifier = Modifier.weight(1f)
                    )
                    TeacherStatCard(
                        label = "Pending Today",
                        value = data?.pendingAttendanceToday?.toString() ?: "-",
                        icon = Icons.Default.Info,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Communication & Quick Actions
                TeacherSectionHeader("Communication & Records")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeacherGridCard(
                        title = "Priority Notices",
                        icon = Icons.Default.Notifications,
                        iconColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.SmartNotices.route) }
                    )
                    TeacherGridCard(
                        title = "My Messages",
                        icon = Icons.Default.Email,
                        iconColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("teacher_messages") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeacherGridCard(
                        title = "Group Chats",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.ChatMain.route) }
                    )
                    TeacherGridCard(
                        title = "My Profile",
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.TeacherProfile.route) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TeacherDashboardCard(
                    title = "Student Directory & Search 🔍",
                    subtitle = "Search any student by Year, Branch & Roll No",
                    icon = Icons.Default.PersonSearch,
                    iconColor = Color(0xFF0284C7),
                    onClick = { navController.navigate(Screen.TeacherStudentLookup.route) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                TeacherDashboardCard(
                    title = "Campus Pulse 📊",
                    subtitle = "View aggregated student wellbeing",
                    icon = Icons.Default.Mood,
                    iconColor = Color(0xFF4F46E5),
                    onClick = { navController.navigate(Screen.CampusPulse.route) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Management & Admin Features
                if (data?.isClassTeacher == true || data?.isHod == true) {
                    TeacherSectionHeader("Management & Leaves")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (data.isClassTeacher) {
                        TeacherDashboardCard(
                            title = "My Class & Proctoring",
                            subtitle = "View your assigned class details",
                            icon = Icons.Default.School,
                            iconColor = Color(0xFF0EA5E9),
                            onClick = { navController.navigate(Screen.MyClass.route) }
                        )
                        TeacherDashboardCard(
                            title = "Section Leaves",
                            subtitle = "Approve or reject student leaves",
                            icon = Icons.Default.Person,
                            iconColor = Color(0xFFF59E0B),
                            onClick = { navController.navigate(Screen.TeacherSectionLeave.route) }
                        )
                    }
                    if (data.isHod) {
                        TeacherDashboardCard(
                            title = "Department Leaves (HOD)",
                            subtitle = "Manage faculty and department leaves",
                            icon = Icons.Default.Domain,
                            iconColor = Color(0xFFEC4899),
                            onClick = { navController.navigate(Screen.TeacherHodLeave.route) }
                        )
                    }
                    TeacherDashboardCard(
                        title = "Manage Students",
                        subtitle = "View and manage student records",
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF6366F1),
                        onClick = { navController.navigate(Screen.ManageStudents.route) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Academic Actions
                TeacherSectionHeader("Academic Duties")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeacherGridCard(
                        title = "Schedule Make-up",
                        icon = Icons.Default.DateRange,
                        iconColor = Color(0xFFF97316),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("schedule_makeup") }
                    )
                    TeacherGridCard(
                        title = "Upload Marks",
                        icon = Icons.Default.Upload,
                        iconColor = Color(0xFF14B8A6),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.UploadMarks.route) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TeacherDashboardCard(
                    title = "Add Quiz Question",
                    subtitle = "Contribute to the daily student quiz",
                    icon = Icons.Default.Add,
                    iconColor = Color(0xFFD946EF),
                    onClick = { navController.navigate(Screen.AddQuizQuestion.route) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Your Assigned Classes
                TeacherSectionHeader("Your Assigned Classes")
                Spacer(modifier = Modifier.height(12.dp))
                data?.assignedSubjects?.forEach { subject ->
                    AssignedSubjectCard(
                        subject = subject,
                        onTakeAttendance = { navController.navigate("attendance/${subject.id}") },
                        onViewHistory = { navController.navigate("teacher_attendance_history/${subject.id}") },
                        onMessageClass = {
                            selectedSubjectForMessage = subject
                            showMessageClassDialog = true
                        }
                    )
                } ?: Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0EA5E9))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showMessageClassDialog && selectedSubjectForMessage != null) {
        val subj = selectedSubjectForMessage!!
        var messageText by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showMessageClassDialog = false },
            title = { Text("Message to ${subj.name}") },
            text = {
                Column {
                    Text("Target: ${subj.department} ${subj.year} ${subj.section}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Announcement Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendSectionMessage(
                                message = messageText, 
                                studentId = null, 
                                department = subj.department, 
                                year = subj.year, 
                                section = subj.section
                            ) {
                                showMessageClassDialog = false
                            }
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Text("Send Broadcast")
                }
            },
            dismissButton = {
                 TextButton(onClick = { showMessageClassDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TeacherSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF334155),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun TeacherStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun TeacherDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TeacherGridCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun AssignedSubjectCard(
    subject: com.auraface.auraface_app.data.network.model.AssignedSubject,
    onTakeAttendance: () -> Unit,
    onViewHistory: () -> Unit,
    onMessageClass: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("Subject ID: ${subject.id}", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0F9FF)
                    ) {
                        Text(
                            text = "${subject.department} • Year ${subject.year} • Sec ${subject.section}", 
                            fontSize = 12.sp, 
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${subject.studentCount} Students", 
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFF475569)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (subject.lastAttendance != null) "Last marked: ${subject.lastAttendance}" else "Not marked yet today",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (subject.lastAttendance != null) Color(0xFF10B981) else Color(0xFFEF4444)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTakeAttendance,
                    modifier = Modifier.weight(1.5f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Attendance", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Text("History", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                
                Surface(
                    onClick = onMessageClass,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Email, contentDescription = "Message Class", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

