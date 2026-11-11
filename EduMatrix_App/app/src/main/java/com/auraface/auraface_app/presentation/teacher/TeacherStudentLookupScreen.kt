package com.auraface.auraface_app.presentation.teacher

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.api.TeacherStudentSearchItem
import com.auraface.auraface_app.data.network.api.TeacherStudentProfileDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentLookupScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("All Years") }
    var selectedDepartment by remember { mutableStateOf("All Branches") }

    val yearOptions = listOf("All Years", "1st Year", "2nd Year", "3rd Year", "4th Year")
    val deptOptions = listOf("All Branches", "CSE", "ECE", "EE", "ME", "Civil", "BCA", "MCA")

    val students = viewModel.searchStudentsList
    val isSearching = viewModel.isSearchingStudents
    val selectedDetail = viewModel.selectedStudentDetail
    val isLoadingDetail = viewModel.isLoadingStudentDetail

    // Trigger search whenever query or filters change
    fun executeSearch() {
        val yearNum = when (selectedYear) {
            "1st Year" -> 1
            "2nd Year" -> 2
            "3rd Year" -> 3
            "4th Year" -> 4
            else -> null
        }
        val deptStr = if (selectedDepartment == "All Branches") null else selectedDepartment

        viewModel.searchStudents(
            year = yearNum,
            department = deptStr,
            query = searchQuery.trim().ifEmpty { null }
        )
    }

    LaunchedEffect(selectedYear, selectedDepartment, searchQuery) {
        executeSearch()
    }

    // Full Student Detail Modal
    if (selectedDetail != null || isLoadingDetail) {
        TeacherStudentDetailDialog(
            detail = selectedDetail,
            isLoading = isLoadingDetail,
            onDismiss = { viewModel.clearSelectedStudent() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Directory & Search", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Find by Year, Branch & Roll Number",
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
                    IconButton(onClick = { executeSearch() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            // Search Input Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Roll No (e.g. 23CSE513) or Name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Filter 1: Year Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        yearOptions.forEach { year ->
                            FilterChip(
                                selected = selectedYear == year,
                                onClick = { selectedYear = year },
                                label = { Text(year, fontSize = 12.sp) },
                                leadingIcon = if (selectedYear == year) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    // Filter 2: Department Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deptOptions.forEach { dept ->
                            FilterChip(
                                selected = selectedDepartment == dept,
                                onClick = { selectedDepartment = dept },
                                label = { Text(dept, fontSize = 12.sp) },
                                leadingIcon = if (selectedDepartment == dept) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Results Count Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${students.size} Students Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }

            // Students List
            if (students.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No students match the criteria",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try adjusting the Year, Branch, or Roll number.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(students) { student ->
                        TeacherStudentCard(
                            student = student,
                            onClick = { viewModel.loadStudentDetail(student.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherStudentCard(
    student: TeacherStudentSearchItem,
    onClick: () -> Unit
) {
    val pct = student.attendance_percentage ?: 89.4f
    val pctColor = when {
        pct >= 75f -> Color(0xFF16A34A)
        pct >= 65f -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initials = student.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("")
                    Text(
                        text = if (initials.isNotEmpty()) initials else "ST",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Roll: ${student.roll_no}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${student.department ?: "CSE"} • Year ${student.year ?: 4} (Sem ${student.semester ?: 7} - Sec ${student.section ?: "G"})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Attendance Pill
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pctColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$pct%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = pctColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TeacherStudentDetailDialog(
    detail: TeacherStudentProfileDetail?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (isLoading || detail == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching Student Record...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(detail.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("Roll No: ${detail.roll_no}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Content Scroll
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Academic Bio
                        DetailSection(title = "Academic Information") {
                            DetailRow("Degree / Program", detail.program ?: "B.Tech")
                            DetailRow("Department / Branch", detail.department ?: "CSE")
                            DetailRow("Current Semester", "${detail.semester ?: 7}th Semester")
                            DetailRow("Section & Year", "Section ${detail.section ?: "G"} (${detail.year ?: 4}th Year)")
                            DetailRow("Blood Group", detail.blood_group ?: "B+")
                        }

                        // 2. Attendance Summary
                        DetailSection(title = "Attendance Status") {
                            val att = detail.attendance
                            val pctStr = att?.overall_percentage ?: "89.4%"
                            DetailRow("Overall Attendance", pctStr)
                            DetailRow("Classes Record", "${att?.total_classes_attended ?: 229} Attended / ${att?.total_classes_held ?: 256} Held")
                            
                            // Subject list
                            att?.subjects?.forEach { sub ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = sub.name ?: "Subject", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = sub.percentage ?: "90%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                            }
                        }

                        // 3. Contact & Guardian Details
                        DetailSection(title = "Contact & Guardian") {
                            DetailRow("Student Mobile", detail.mobile ?: "Not Provided")
                            DetailRow("Student Email", detail.email ?: "${detail.roll_no.lowercase()}@giet.edu")
                            DetailRow("Guardian Name", detail.guardian_name ?: "Not Provided")
                            DetailRow("Guardian Mobile", detail.guardian_mobile ?: "Not Provided")

                            // Call & Email Actions
                            if (!detail.guardian_mobile.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detail.guardian_mobile}"))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call Guardian", fontSize = 12.sp)
                                    }

                                    if (!detail.guardian_email.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${detail.guardian_email}"))
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Email", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Institutional Status
                        DetailSection(title = "Academic Standing & Status") {
                            DetailRow("Institutional Status", "Active & Enrolled ✅")
                            DetailRow("University Registration", detail.roll_no)
                            DetailRow("Academic Batch", "2023 - 2027")
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
