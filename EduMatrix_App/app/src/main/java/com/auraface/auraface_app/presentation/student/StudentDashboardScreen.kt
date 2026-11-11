@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.student

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.auraface.auraface_app.R
import androidx.compose.ui.res.painterResource
import com.auraface.auraface_app.data.network.RetrofitClient
import com.auraface.auraface_app.data.remote.dto.UserProfile
import com.auraface.auraface_app.data.network.model.GamificationProfile
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun StudentDashboardScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val analytics = viewModel.analytics 
    val gamificationProfile = viewModel.gamificationProfile
    var showIdDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadAttendanceHistory()
        viewModel.loadProfile()
        viewModel.loadGamificationProfile()
    }

    if (showIdDialog) {
        DigitalIdCardDialog(
            profile = viewModel.profile,
            gamificationProfile = gamificationProfile,
            onDismiss = { showIdDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Progress", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4F46E5) // Indigo
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
            // Stats Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Overall Attendance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (analytics != null) {
                        Text(
                            text = "${analytics.overallAttendance}%",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { analytics.overallAttendance / 100f },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (analytics.overallAttendance >= 75) Color(0xFF10B981) else Color(0xFFEF4444),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    } else {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading Statistics...", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                
                DashboardSectionHeader("My Achievements \uD83C\uDFC6")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val streak = gamificationProfile?.current_streak ?: 0
                    AchievementBadge(
                        icon = Icons.Default.LocalFireDepartment,
                        title = if (streak >= 7) "Streak Master" else "Active",
                        subtitle = "$streak Days",
                        tint = if (streak > 0) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        background = if (streak > 0) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)
                    )
                    
                    val attendance = analytics?.overallAttendance ?: 0f
                    AchievementBadge(
                        icon = Icons.Default.WorkspacePremium,
                        title = if (attendance >= 95f) "Perfect Week" else "Attentive",
                        subtitle = "${attendance.toInt()}% Att.",
                        tint = if (attendance >= 75f) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                        background = if (attendance >= 75f) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                    )
                    
                    val title = gamificationProfile?.title ?: "Beginner"
                    val level = gamificationProfile?.current_level ?: 1
                    AchievementBadge(
                        icon = Icons.Default.EmojiEvents,
                        title = title,
                        subtitle = "Level $level",
                        tint = Color(0xFF8B5CF6),
                        background = Color(0xFFEDE9FE)
                    )
                    
                    val xp = gamificationProfile?.total_xp ?: 0
                    AchievementBadge(
                        icon = Icons.Default.Star,
                        title = "Quiz Pro",
                        subtitle = "$xp XP",
                        tint = if (xp > 0) Color(0xFF10B981) else Color(0xFF94A3B8),
                        background = if (xp > 0) Color(0xFFD1FAE5) else Color(0xFFF1F5F9)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                DashboardSectionHeader("Core Academics")
                Spacer(modifier = Modifier.height(12.dp))
                StudentDashboardCard(
                    title = "Academic Ledger & Records 📊",
                    subtitle = "Subject attendance, internal marks & fee dues",
                    icon = Icons.Default.Assessment,
                    iconColor = Color(0xFF16A34A),
                    onClick = { navController.navigate(Screen.GietErpDashboard.route) }
                )
                StudentDashboardCard(
                    title = "Digital Campus ID",
                    subtitle = "View your smart ID card",
                    icon = Icons.Default.Badge,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { showIdDialog = true }
                )
                StudentDashboardCard(
                    title = "View Timetable",
                    subtitle = "Check your daily class schedule",
                    icon = Icons.Default.DateRange,
                    iconColor = Color(0xFF3B82F6),
                    onClick = { navController.navigate("student_schedule") }
                )
                StudentDashboardCard(
                    title = "Attendance History",
                    subtitle = "Detailed subject-wise records",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF10B981),
                    onClick = { navController.navigate("my_attendance") }
                )
                StudentDashboardCard(
                    title = "Request Leave",
                    subtitle = "Apply for official absences",
                    icon = Icons.Default.ExitToApp,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { navController.navigate(Screen.StudentLeave.route) }
                )
                StudentDashboardCard(
                    title = "GIET Fees Payment",
                    subtitle = "Tuition, hostel & exam fee portal",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = Color(0xFF4F46E5),
                    onClick = { navController.navigate(Screen.FeePayment.route) }
                )

                Spacer(modifier = Modifier.height(24.dp))
                DashboardSectionHeader("Performance & Reports")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudentGridCard(
                        title = "Exam Schedule",
                        icon = Icons.Default.EventNote,
                        iconColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.ExamSchedule.route) }
                    )
                    StudentGridCard(
                        title = "Exam Results",
                        icon = Icons.Default.BarChart,
                        iconColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.ExamResults.route) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudentGridCard(
                        title = "CGPA Calc",
                        icon = Icons.Default.Calculate,
                        iconColor = Color(0xFFF97316),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.CGPACalculator.route) }
                    )
                    StudentGridCard(
                        title = "Proctor History",
                        icon = Icons.Default.AssignmentInd,
                        iconColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("student_proctor") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                DashboardSectionHeader("Smart Features")
                Spacer(modifier = Modifier.height(12.dp))
                
                StudentDashboardCard(
                    title = "Smart E-Library",
                    subtitle = "Access books, notes, and past papers",
                    icon = Icons.Default.MenuBook,
                    iconColor = Color(0xFF10B981),
                    onClick = { navController.navigate(Screen.SmartLibrary.route) }
                )
                StudentDashboardCard(
                    title = "Library Seat Allocation",
                    subtitle = "Book study desks and computer seats",
                    icon = Icons.Default.EventSeat,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { navController.navigate(Screen.LibrarySeatAllocation.route) }
                )
                StudentDashboardCard(
                    title = "EduMatrix Spaces \uD83C\uDFE2",
                    subtitle = "Book campus study rooms & facilities",
                    icon = Icons.Default.MeetingRoom,
                    iconColor = Color(0xFF6366F1),
                    onClick = { navController.navigate(Screen.AuraSpaces.route) }
                )
                StudentDashboardCard(
                    title = "EduMatrix Found \uD83D\uDD0D",
                    subtitle = "Campus Lost & Found",
                    icon = Icons.Default.Search,
                    iconColor = Color(0xFF0F766E),
                    onClick = { navController.navigate(Screen.AuraFound.route) }
                )
                StudentDashboardCard(
                    title = "Ask EduMatrix (AI Chat)",
                    subtitle = "Your smart academic assistant",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Color(0xFFD946EF),
                    onClick = { navController.navigate(Screen.AuraChat.route) }
                )
                StudentDashboardCard(
                    title = "AI Study Planner",
                    subtitle = "Personalized automatic study schedule",
                    icon = Icons.Default.School,
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { navController.navigate("study_planner") }
                )
                StudentDashboardCard(
                    title = "Academic Calendar",
                    subtitle = "Classes, exams, holidays synced",
                    icon = Icons.Default.CalendarMonth,
                    iconColor = Color(0xFF10B981),
                    onClick = { navController.navigate("academic_calendar") }
                )
                StudentDashboardCard(
                    title = "Teacher Availability",
                    subtitle = "Check if teachers are free or busy",
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { navController.navigate("teacher_availability") }
                )
                StudentDashboardCard(
                    title = "EduMatrix Pulse \uD83D\uDC93",
                    subtitle = "Log your daily mood & focus",
                    icon = Icons.Default.Mood,
                    iconColor = Color(0xFFDB2777),
                    onClick = { navController.navigate(Screen.AuraPulse.route) }
                )
                StudentDashboardCard(
                    title = "EduMatrix Quests \uD83C\uDFC6",
                    subtitle = "Daily challenges for XP",
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { navController.navigate(Screen.AuraQuests.route) }
                )
                StudentDashboardCard(
                    title = "EduMatrix Bites \uD83C\uDF54",
                    subtitle = "Smart Canteen & Live Food Status",
                    icon = Icons.Default.Fastfood,
                    iconColor = Color(0xFFE11D48),
                    onClick = { navController.navigate(Screen.AuraBites.route) }
                )
                StudentDashboardCard(
                    title = "EduMatrix Focus Flow \u23F1\uFE0F",
                    subtitle = "Deep work sessions for XP",
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFF38BDF8),
                    onClick = { navController.navigate(Screen.AuraFocus.route) }
                )
                StudentDashboardCard(
                    title = "Attendance Insights",
                    subtitle = "AI-driven analysis of attendance",
                    icon = Icons.Default.Insights,
                    iconColor = Color(0xFF0EA5E9),
                    onClick = { navController.navigate(Screen.SmartInsights.route) }
                )
                StudentDashboardCard(
                    title = "Priority Notices",
                    subtitle = "Important circulars & broadcasts",
                    icon = Icons.Default.NotificationsActive,
                    iconColor = Color(0xFFEF4444),
                    onClick = { navController.navigate(Screen.SmartNotices.route) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudentGridCard(
                        title = "My Messages",
                        icon = Icons.Default.Email,
                        iconColor = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("student_messages") }
                    )
                    StudentGridCard(
                        title = "Class Group Chat",
                        icon = Icons.Default.Forum,
                        iconColor = Color(0xFF14B8A6),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.ChatMain.route) }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                DashboardSectionHeader("Placement & Career Readiness")
                Spacer(modifier = Modifier.height(12.dp))
                
                StudentDashboardCard(
                    title = "Placement Readiness",
                    subtitle = "Track your career readiness",
                    icon = Icons.Default.Work,
                    iconColor = Color(0xFF64748B),
                    onClick = { navController.navigate("placement_readiness") }
                )
                StudentDashboardCard(
                    title = "AI Mock Interview",
                    subtitle = "Practice 1-on-1 virtual interviews with Ask EduMatrix AI",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { navController.navigate(Screen.AIInterview.route) }
                )
                StudentDashboardCard(
                    title = "AI Mock Test Center",
                    subtitle = "Standardized subject and aptitude tests",
                    icon = Icons.Default.Assessment,
                    iconColor = Color(0xFF0EA5E9),
                    onClick = { navController.navigate(Screen.MockTest.route) }
                )
                StudentDashboardCard(
                    title = "Resume CV Builder",
                    subtitle = "Generate industry-standard professional resumes",
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFF10B981),
                    onClick = { navController.navigate(Screen.ResumeBuilder.route) }
                )
                StudentDashboardCard(
                    title = "Rewards Store \uD83C\uDF81",
                    subtitle = "Redeem your XP for real-world rewards",
                    icon = Icons.Default.CardGiftcard,
                    iconColor = Color(0xFFD946EF),
                    onClick = { navController.navigate(Screen.RewardsStore.route) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                DashboardSectionHeader("Campus Entertainment")
                Spacer(modifier = Modifier.height(12.dp))
                
                StudentDashboardCard(
                    title = "Campus Movies \uD83C\uDF7F",
                    subtitle = "Book tickets for weekend campus screenings",
                    icon = Icons.Default.Movie,
                    iconColor = Color(0xFFEF4444),
                    onClick = { navController.navigate("campus_movies") }
                )
                
                StudentDashboardCard(
                    title = "EduMatrix Sports \uD83C\uDFC6",
                    subtitle = "Live scores and upcoming college matches",
                    icon = Icons.Default.SportsBasketball,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { navController.navigate("campus_sports") }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DashboardSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF334155),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun StudentDashboardCard(
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
fun StudentGridCard(
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
fun DigitalIdCardDialog(profile: UserProfile?, gamificationProfile: GamificationProfile?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600)
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(480.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { flipped = !flipped }
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
            ) {
                if (rotation <= 90f) {
                    IdCardFront(profile, gamificationProfile)
                } else {
                    Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                        IdCardBack(profile, gamificationProfile)
                    }
                }
            }
        }
    }
}

@Composable
fun IdCardFront(profile: UserProfile?, gamificationProfile: GamificationProfile?) {
   AnimatedAuraWrapper(gamificationProfile) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
                        )
                    )
            ) {
                // Background pattern
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f))
                )
                
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Space Placeholder
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_playstore),
                        contentDescription = "University Logo",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp),
                        contentScale = ContentScale.Inside
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            "GANDHI INSTITUTE OF ENGINEERING AND TECHNOLOGY",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                        Text(
                            "UNIVERSITY, ODISHA",
                            color = Color.White.copy(0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            "STUDENT IDENTITY CARD",
                            color = Color(0xFFFFD700), // Gold
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // Profile Info Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image overlapping header effect
                Box(
                    modifier = Modifier
                        .offset(y = (-20).dp)
                        .size(110.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(3.dp, Color(0xFF0D47A1), RoundedCornerShape(12.dp))
                ) {
                    if (profile?.profile_image != null) {
                        AsyncImage(
                            model = RetrofitClient.BASE_URL + profile.profile_image.removePrefix("/"),
                            contentDescription = "Profile Pic",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.fillMaxSize().padding(20.dp), tint = Color(0xFF94A3B8))
                    }
                }
                
                Column(
                    modifier = Modifier.offset(y = (-10).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = profile?.name?.uppercase() ?: "UNKNOWN STUDENT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Card Details
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IdCardDetailRow("Program:", profile?.program ?: "N/A")
                        IdCardDetailRow("Branch/Dept:", profile?.department ?: "CSE")
                        IdCardDetailRow("Regd/Roll No:", profile?.roll_no ?: "N/A")
                        
                        val semString = if (profile?.semester != null) {
                            "Semester ${profile.semester}"
                        } else if (profile?.year != null) {
                            "Year ${profile.year}"
                        } else {
                            "Semester - N/A"
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IdCardDetailRow("Session:", semString, modifier = Modifier.weight(1f))
                            IdCardDetailRow("Blood Grp:", profile?.blood_group ?: "N/A", modifier = Modifier.weight(0.8f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap to flip", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
   }
}

@Composable
fun IdCardDetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
fun IdCardBack(profile: UserProfile?, gamificationProfile: GamificationProfile?) {
    val context = LocalContext.current
   AnimatedAuraWrapper(gamificationProfile) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Magnetic Stripe representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .padding(top = 16.dp)
                    .background(Color(0xFF1E293B))
            )
            
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "TERMS & CONDITIONS", 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 12.sp, 
                    color = Color(0xFF0D47A1),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val terms = listOf(
                    "This card is the property of the University and must be returned upon graduation or withdrawal.",
                    "This card is non-transferable and must be presented upon request by university officials.",
                    "In case of loss or damage, report immediately to the Administration Office."
                )
                
                terms.forEachIndexed { index, term ->
                    Row(modifier = Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.Top) {
                        Text("${index + 1}.", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                        Text(term, fontSize = 10.sp, color = Color(0xFF64748B), lineHeight = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Emergency info & Address
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EMERGENCY CONTACT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF0D47A1))
                        Text(profile?.guardian_name ?: "Guardian", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                        val gPhone = profile?.guardian_mobile
                        if (!gPhone.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$gPhone"))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(gPhone, fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("N/A", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ISSUING AUTHORITY", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF0D47A1))
                        Text("Dean of Students", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                        Text("Gandhi Inst. of Engg. & Tech.", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Faux Barcode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val widths = remember { List(41) { (1..3).random().dp } }
                        Row(modifier = Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.Center) {
                            widths.forEach { width ->
                                Box(modifier = Modifier.width(width).fillMaxHeight().background(Color.Black))
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            profile?.roll_no ?: "0000000000", 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 4.sp, 
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap to flip", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
   }
}

@Composable
fun IdCardField(label: String, value: String, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
    }
}

@Composable
fun AchievementBadge(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    background: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = tint, spotColor = tint)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, background)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnimatedAuraWrapper(
    gamificationProfile: GamificationProfile?,
    content: @Composable () -> Unit
) {
    val level = gamificationProfile?.current_level ?: 1
    
    val auraColors = when (level) {
        4 -> listOf(Color(0xFFFFD700), Color(0xFFFF8C00), Color(0xFFFF1493), Color(0xFF00BFFF), Color(0xFFFFD700)) // Elite: Rainbow/Gold
        3 -> listOf(Color(0xFF8B5CF6), Color(0xFFD946EF), Color(0xFF8B5CF6)) // Advanced: Purple/Pink
        2 -> listOf(Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF3B82F6)) // Intermediate: Blue/Cyan
        else -> listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFE2E8F0)) // Beginner: Silver/Gray
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .drawBehind {
                rotate(rotation) {
                    drawRect(
                        brush = Brush.sweepGradient(auraColors),
                        topLeft = Offset(-size.width, -size.height),
                        size = Size(size.width * 3f, size.height * 3f)
                    )
                }
            }
            .padding(if (level >= 3) 5.dp else 2.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
