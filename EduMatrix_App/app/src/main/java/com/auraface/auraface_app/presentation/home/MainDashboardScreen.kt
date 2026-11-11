package com.auraface.auraface_app.presentation.home

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.auraface.auraface_app.core.navigation.Screen
import com.auraface.auraface_app.data.network.RetrofitClient
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.auraface.auraface_app.R
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val role = viewModel.getUserRole()?.lowercase() ?: "unknown"
    val username = viewModel.getUsername() ?: role.replaceFirstChar { it.uppercase() }

    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutEduMatrixDialog(onDismiss = { showAboutDialog = false })
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Light Slate
            .verticalScroll(rememberScrollState())
    ) {
        // Premium Vibrant Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4F46E5), Color(0xFF0EA5E9)) // Indigo to Light Blue
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                )
                .padding(24.dp)
        ) {
            val profileRoute = when (role) {
                "student" -> Screen.StudentProfile.route
                "teacher" -> Screen.TeacherProfile.route
                "admin" -> "admin_profile"
                else -> null
            }

            if (profileRoute != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { navController.navigate(profileRoute) },
                    contentAlignment = Alignment.Center
                ) {
                    val profile = viewModel.profile
                    if (profile?.profile_image != null) {
                        AsyncImage(
                            model = RetrofitClient.BASE_URL + profile.profile_image.removePrefix("/"),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = "EduMatrix Platform",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hello, $username",
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                var menuExpanded by remember { mutableStateOf(false) }
                
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp).clickable { menuExpanded = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("System Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                    
                    val profileRouteInner = when (role) {
                        "student" -> Screen.StudentProfile.route
                        "teacher" -> Screen.TeacherProfile.route
                        "admin" -> "admin_profile"
                        else -> null
                    }
                    if (profileRouteInner != null) {
                        DropdownMenuItem(
                            text = { Text("My Profile") },
                             leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                navController.navigate(profileRouteInner)
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("About EduMatrix") },
                         leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            showAboutDialog = true
                        }
                    )
                    
                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                         leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Dashboard Modules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Role-Based Navigation Cards
            if (role == "admin") {
                FeatureCardPremium(
                    title = "Admin Control Centre",
                    subtitle = "Manage users, subjects, and system analytics",
                    icon = Icons.Default.AdminPanelSettings,
                    gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                ) { navController.navigate(Screen.AdminDashboard.route) }
            } else if (role == "teacher") {
                FeatureCardPremium(
                    title = "Faculty Dashboard",
                    subtitle = "Mark attendance, manage curriculum, and students",
                    icon = Icons.Default.CoPresent,
                    gradientColors = listOf(Color(0xFF0EA5E9), Color(0xFF3B82F6))
                ) { navController.navigate(Screen.TeacherDashboard.route) }
            } else if (role == "student") {
                FeatureCardPremium(
                    title = "My Academic Progress",
                    subtitle = "View attendance, grades, and insights",
                    icon = Icons.Default.School,
                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF14B8A6))
                ) { navController.navigate(Screen.StudentDashboard.route) }
            }

            FeatureCardPremium(
                title = "Institute Gallery",
                subtitle = "Browse events, highlights, and campus photos",
                icon = Icons.Default.PhotoLibrary,
                gradientColors = listOf(Color(0xFFE91E63), Color(0xFFF43F5E)),
                onClick = { navController.navigate("gallery") }
            )
            
            if (role == "student") {
                 FeatureCardPremium(
                    title = "Placement Tracker",
                    subtitle = "Calculate your corporate readiness score",
                    icon = Icons.Default.Work,
                    gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFF97316)),
                    onClick = { navController.navigate("placement_readiness") }
                )
            }
            
            FeatureCardPremium(
                 title = "Brain Teaser",
                 subtitle = "Play the daily quiz game and test your knowledge",
                 icon = Icons.Default.VideogameAsset,
                 gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)),
                 onClick = { navController.navigate("quiz_game") }
             )

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Footer
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))),
                onClick = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color(0xFF475569))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Secure Logout", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun FeatureCardPremium(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush = Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9), // Slate Light
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AboutEduMatrixDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
        val yOffsetAnim = remember { androidx.compose.animation.core.Animatable(50f) }
        
        LaunchedEffect(Unit) {
            alphaAnim.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(800))
        }
        LaunchedEffect(Unit) {
            yOffsetAnim.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF312E81)) // Deep Blue to Royal Purple
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .graphicsLayer(alpha = alphaAnim.value, translationY = yOffsetAnim.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                         Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Text(
                    text = "Smart Institutions Need Smart Systems.",
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // App Logo with glow
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(
                            elevation = 30.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color(0xFF38BDF8),
                            spotColor = Color(0xFF818CF8)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_playstore),
                        contentDescription = "App Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "🌌 EduMatrix",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The Future of Smart Attendance",
                    fontSize = 18.sp,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🚀 Redefining Presence with Intelligence",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                AboutSectionText("EduMatrix is an AI-driven facial recognition platform designed to revolutionize attendance management across educational institutions.\n\nBy combining real-time computer vision, secure backend architecture, and intelligent analytics, EduMatrix eliminates manual processes and transforms attendance into a seamless digital experience.")

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "No paperwork.\nNo proxies.\nNo inefficiencies.\n\nJust intelligent automation.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(32.dp))

                AboutHeading("🧠 Powered by Advanced AI")
                AboutSectionText("EduMatrix uses deep learning–based facial recognition algorithms to:")
                Spacer(modifier = Modifier.height(12.dp))
                AboutBulletPoint("Detect faces in real time")
                AboutBulletPoint("Verify identity with high accuracy")
                AboutBulletPoint("Prevent proxy attendance")
                AboutBulletPoint("Log attendance instantly in the database")
                Spacer(modifier = Modifier.height(16.dp))
                AboutSectionText("Our system ensures reliability, speed, and precision in every scan.")

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(32.dp))

                AboutHeading("⚡ What Makes EduMatrix Different?")
                Spacer(modifier = Modifier.height(16.dp))

                AboutFeatureCard("✨ AI-First Architecture", "Built with intelligent automation at its core.")
                AboutFeatureCard("🔐 Secure by Design", "JWT authentication, encrypted communication, and protected access control.")
                AboutFeatureCard("📊 Data-Driven Insights", "Interactive dashboards with attendance trends and analytics.")
                AboutFeatureCard("🔔 Real-Time Communication", "Emergency alerts and announcements via Firebase Cloud Messaging.")
                AboutFeatureCard("🏫 Multi-Role Ecosystem", "Separate dashboards for Admins, Teachers, and Students.")
                AboutFeatureCard("📈 Scalable Backend", "FastAPI-powered backend with optimized database handling.")

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(32.dp))

                AboutHeading("🎯 Our Mission")
                AboutSectionText("To build intelligent institutional systems that increase transparency, eliminate inefficiencies, and empower organizations through automation.")

                Spacer(modifier = Modifier.height(24.dp))

                AboutHeading("🌍 Our Vision")
                AboutSectionText("A future where AI seamlessly integrates into everyday academic operations — enhancing productivity, accuracy, and trust.")

                Spacer(modifier = Modifier.height(24.dp))

                AboutHeading("🛡️ Security & Privacy Commitment")
                AboutSectionText("EduMatrix prioritizes data protection.\nFacial data is processed securely, authentication is encrypted, and access is strictly role-controlled.\n\nWe believe innovation must always respect privacy.")

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(32.dp))

                AboutHeading("🏗 Technology Behind EduMatrix")
                Spacer(modifier = Modifier.height(16.dp))
                val techStack = listOf(
                    "Android (Jetpack Compose UI)",
                    "FastAPI (High-Performance Backend)",
                    "SQLite Database",
                    "Python + OpenCV (Face Recognition Engine)",
                    "Firebase Cloud Messaging",
                    "JWT Authentication"
                )
                
                techStack.forEach { tech ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp)
                    ) {
                        Text(text = "• $tech", color = Color(0xFFE2E8F0), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                
                // Crafted With Precision
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👨‍💻 Crafted With Precision", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Developed by", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        Text("Ranjeet Singh", color = Color(0xFF38BDF8), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 4.dp))
                        Text("B.Tech CSE | AI & System Design Enthusiast", color = Color(0xFFE2E8F0), fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("EduMatrix – 2026 Edition", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("📌 Version", color = Color(0xFF94A3B8), fontSize = 14.sp)
                Text("EduMatrix v1.0.0", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                Text("© 2026 EduMatrix Technologies. All Rights Reserved.", color = Color(0xFF64748B), fontSize = 12.sp, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun AboutHeading(text: String) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun AboutSectionText(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        color = Color(0xFFCBD5E1),
        lineHeight = 24.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun AboutBulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = Color(0xFFE2E8F0), fontSize = 15.sp)
    }
}

@Composable
fun AboutFeatureCard(title: String, desc: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(desc, color = Color(0xFF94A3B8), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}


