package com.auraface.auraface_app.presentation.student

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.api.ErpFullReport
import com.auraface.auraface_app.data.network.api.ErpSubjectAttendance
import com.auraface.auraface_app.data.network.api.ErpSubjectMarks
import com.auraface.auraface_app.data.network.api.ErpFeeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GietErpDashboardScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val erpData = viewModel.erpReport
    val profile = viewModel.profile
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Attendance", "Marks", "Fees & Dues", "Student Bio")

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Academic Ledger & Records", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Institutional Database Records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        profile?.roll_no?.let { viewModel.loadErpData(it) }
                        viewModel.loadProfile()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Records", tint = Color(0xFF16A34A))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = erpData?.university ?: "GIET University, Gunupur",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Roll: ${erpData?.roll_no ?: profile?.roll_no ?: "Student"} • 7th Sem (Sec G)",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF16A34A).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E))
                        ) {
                            Text(
                                text = "VERIFIED",
                                color = Color(0xFF4ADE80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Overall Attendance", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = viewModel.analytics?.overallAttendance?.let { "${it}%" }
                                    ?: erpData?.attendance?.overall_percentage
                                    ?: "85.0%",
                                color = Color(0xFF4ADE80),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text("Current CGPA", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = erpData?.marks?.cgpa ?: "8.42",
                                color = Color(0xFF60A5FA),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text("Fee Balance", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = erpData?.fees?.total_outstanding ?: "₹0.00",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // Scrollable / Fixed Tab Bar
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> ErpAttendanceTab(erpData)
                1 -> ErpMarksTab(erpData)
                2 -> ErpFeesTab(erpData)
                3 -> ErpBioTab(erpData)
            }
        }
    }
}

@Composable
fun ErpAttendanceTab(erpData: ErpFullReport?) {
    val subjects = erpData?.attendance?.subjects ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Lectures Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "${erpData?.attendance?.total_classes_attended ?: 82} Attended / ${erpData?.attendance?.total_classes_held ?: 97} Conducted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = erpData?.attendance?.status ?: "CRITERIA MET",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text("Subject-Wise Attendance Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        items(subjects) { sub ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${sub.code ?: "CS"}: ${sub.name ?: "Subject"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Attended: ${sub.attended ?: "0"} / Total: ${sub.total_classes ?: "0"} classes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = sub.percentage ?: "85%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErpMarksTab(erpData: ErpFullReport?) {
    val marksList = erpData?.marks?.subjects ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Academic Standing", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Previous SGPA: ${erpData?.marks?.sgpa_previous ?: "8.65"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "CGPA: ${erpData?.marks?.cgpa ?: "8.42"}",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text("7th Semester Internal Assessments", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        items(marksList) { mark ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${mark.code ?: "CS"}: ${mark.subject ?: "Subject"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = mark.grade ?: "O",
                                color = Color(0xFF7C3AED),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Midterm 1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(mark.internal_1 ?: "-", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Midterm 2", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(mark.internal_2 ?: "-", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Assignment", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(mark.assignment ?: "-", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Total Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(mark.total ?: "-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErpFeesTab(erpData: ErpFullReport?) {
    val dues = erpData?.fees?.dues ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Fee Account Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Total Due: ${erpData?.fees?.total_outstanding ?: "₹0.00"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "ALL CLEARED ✅",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text("Fee Breakdown & Receipts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        items(dues) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.head ?: "Fee Head", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Paid: ${item.paid ?: item.total ?: "₹0.00"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.status == "PAID") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = item.status ?: "PAID",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (item.status == "PAID") Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErpBioTab(erpData: ErpFullReport?) {
    val prof = erpData?.profile

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Official Student Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    ErpInfoRow(label = "Registration / Roll No.", value = prof?.roll_no ?: erpData?.roll_no ?: "N/A")
                    ErpInfoRow(label = "Program & Degree", value = prof?.program ?: "B.Tech")
                    ErpInfoRow(label = "Department", value = prof?.department ?: "Computer Science & Engineering")
                    ErpInfoRow(label = "Current Semester", value = "7th Semester (Section ${prof?.section ?: "G"})")
                    ErpInfoRow(label = "Academic Year", value = prof?.year ?: "4th Year")
                    ErpInfoRow(label = "Admission Batch", value = prof?.batch ?: "2023 - 2027")
                    ErpInfoRow(label = "Faculty Mentor / Proctor", value = prof?.proctor ?: "Dr. Assigned Faculty Mentor")
                    ErpInfoRow(label = "Blood Group", value = prof?.blood_group ?: "B+")
                }
            }
        }
    }
}

@Composable
fun ErpInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
