package com.auraface.auraface_app.presentation.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.DefaulterStudent
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultersListScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val defaulters = viewModel.defaulterCount?.students ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Defaulters", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFDC2626) // Red Header for Warning
                ),
                actions = {
                    IconButton(onClick = {
                        val ids = defaulters.map { it.id }
                        viewModel.notifyHodsAboutDefaulters(ids,
                            onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Send Mass Warning", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (defaulters.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val ids = defaulters.map { it.id }
                        viewModel.notifyHodsAboutDefaulters(ids,
                            onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    },
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    text = { Text("Alert All Defaulters") },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (defaulters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Defaulters Found",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("All students have attendance above 75%.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                item {
                    Text(
                        "Showing ${defaulters.size} students with critically low attendance. Action required.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(defaulters) { student ->
                    DefaulterStudentCard(student = student) { action ->
                        when (action) {
                            "CALL" -> {
                                val phone = student.guardian_mobile?.trim()
                                if (!phone.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$phone")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No guardian phone on record for ${student.name}.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            "EMAIL" -> {
                                val email = student.guardian_email?.trim() ?: "${student.rollNo.lowercase()}@giet.edu"
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$email")
                                    putExtra(Intent.EXTRA_SUBJECT, "URGENT: Low Attendance Warning for ${student.name}")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Dear ${student.guardian_name ?: "Parent/Guardian"},\n\n" +
                                        "This is an urgent academic notice regarding your ward, ${student.name} (${student.rollNo}).\n\n" +
                                        "Their cumulative attendance is currently ${student.percentage}%, which is critically below the mandatory 75% threshold.\n" +
                                        "Please contact the HOD/Proctor office immediately."
                                    )
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefaulterStudentCard(
    student: DefaulterStudent,
    onAction: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = Color(0xFFFEF2F2)
                    ) {
                        Center {
                            Text(
                                text = student.name.take(1).uppercase(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = student.rollNo,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "${student.department} • Year ${student.year} • Sec ${student.section}", 
                                fontSize = 11.sp, 
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!student.guardian_name.isNullOrBlank() || !student.guardian_mobile.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${student.guardian_name ?: "Guardian"}: ${student.guardian_mobile ?: "N/A"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Severity Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF2F2)
                ) {
                    Text(
                        text = "${student.percentage}%", 
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFFDC2626)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Interactive Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction("CALL") },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A))
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Guardian", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = { onAction("EMAIL") },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Warning", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun Center(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        content()
    }
}
