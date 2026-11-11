@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val analytics = viewModel.dashboardAnalytics

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Control Centre", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1) // Indigo Primary
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
            // Gradient Premium Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "System Overview",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    analytics?.roleAnalytics?.let { stats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminStatCard("Students", stats.totalStudents.toString(), Icons.Default.Person, Modifier.weight(1f))
                            AdminStatCard("Teachers", stats.totalTeachers.toString(), Icons.Default.Face, Modifier.weight(1f))
                            AdminStatCard("Classes", stats.activeClasses.toString(), Icons.Default.Home, Modifier.weight(1f))
                        }
                    } ?: CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                
                AdminSectionHeader("Smart Admin Insights")
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Defaulter Analysis Card
                    val count = viewModel.defaulterCount
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate(Screen.AdminDefaulters.route) },
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)) // Red Light
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.padding(10.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Defaulters", style = MaterialTheme.typography.labelLarge, color = Color(0xFF991B1B))
                            Text("${count?.defaulters ?: "N/A"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF7F1D1D))
                            Text("Below 75%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                        }
                    }

                    // Critical Subjects Card
                    val mostAbsent = viewModel.mostAbsentSubjects.firstOrNull()
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)) // Orange Light
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(0xFFF97316).copy(alpha = 0.15f)
                            ) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.padding(10.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Lowest Attendance", style = MaterialTheme.typography.labelLarge, color = Color(0xFF9A3412))
                            Text(mostAbsent?.subject ?: "N/A", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, color = Color(0xFF7C2D12))
                            Text("${mostAbsent?.percentage ?: 0.0}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEA580C))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                AdminDashboardCard(
                    title = "Campus Pulse Heatmap \uD83D\uDCCA",
                    subtitle = "Monitor overall student & faculty wellbeing",
                    icon = Icons.Default.Mood,
                    iconColor = Color(0xFFDB2777)
                ) { navController.navigate(Screen.CampusPulse.route) }

                Spacer(modifier = Modifier.height(24.dp))
                AdminSectionHeader("Management Tools")
                Spacer(modifier = Modifier.height(12.dp))
                
                AdminDashboardCard("Manage Students", "User accounts & enrollment", Icons.Default.Person, Color(0xFF3B82F6)) { navController.navigate(Screen.ManageStudents.route) }
                AdminDashboardCard("Manage Teachers", "Faculty accounts & assignments", Icons.Default.Face, Color(0xFF10B981)) { navController.navigate(Screen.ManageTeachers.route) }
                AdminDashboardCard("Manage Subjects", "Curriculum structure", Icons.AutoMirrored.Filled.List, Color(0xFF8B5CF6)) { navController.navigate(Screen.ManageSubjects.route) }
                AdminDashboardCard("Manage Timetable", "Class scheduling across institute", Icons.Default.DateRange, Color(0xFFF59E0B)) { navController.navigate(Screen.ManageTimetable.route) }
                AdminDashboardCard("Manage Calendar", "Update academic calendar events", Icons.Default.DateRange, Color(0xFFE11D48)) { navController.navigate("admin_calendar") }
                
                Spacer(modifier = Modifier.height(24.dp))
                AdminSectionHeader("Communication & Reports")
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminGridCard("Group Notice", Icons.Default.Notifications, Color(0xFFEF4444), Modifier.weight(1f)) { navController.navigate(Screen.SendNotice.route) }
                    AdminGridCard("Notice Board", Icons.Default.Info, Color(0xFF0EA5E9), Modifier.weight(1f)) { navController.navigate(Screen.SmartNotices.route) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminGridCard("Detailed Reports", Icons.AutoMirrored.Filled.List, Color(0xFF14B8A6), Modifier.weight(1f)) { navController.navigate(Screen.AdminReports.route) }
                    AdminGridCard("Live Tracking", Icons.Default.Monitor, Color(0xFFF97316), Modifier.weight(1f)) { navController.navigate("real_time_monitoring") }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                AdminSectionHeader("System Administration")
                Spacer(modifier = Modifier.height(12.dp))
                
                AdminDashboardCard("Add Quiz Question", "Update the daily brain teaser", Icons.Default.Add, Color(0xFFD946EF)) { navController.navigate(Screen.AddQuizQuestion.route) }
                AdminDashboardCard("System Settings", "Configure application parameters", Icons.Default.Settings, Color(0xFF64748B)) { navController.navigate(Screen.Settings.route) }

                Spacer(modifier = Modifier.height(32.dp))

                // Logout Footer
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))),
                    onClick = {
                        navController.navigate(Screen.MainDashboard.route) {
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color(0xFF475569))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Back to Main Dashboard", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AdminSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF334155),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun AdminStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
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
fun AdminDashboardCard(
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
fun AdminGridCard(
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
