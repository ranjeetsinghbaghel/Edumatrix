@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun SubjectHistoryScreen(
    navController: NavController,
    subjectId: String,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val history = viewModel.subjectHistory
    val context = LocalContext.current

    LaunchedEffect(subjectId) {
        viewModel.loadSubjectHistory(subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History: $subjectId") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No attendance records for this subject.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { session ->
                    SessionHistoryCard(session, subjectId, context)
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(session: Map<String, Any>, subjectId: String, context: android.content.Context) {
    val date = session["date"]?.toString() ?: ""
    val period = session["period"]?.toString() ?: ""
    
    // Numbers from JSON map via Retrofit evaluate loosely as Double initially
    val presentCount = (session["total_present"] as? Number)?.toInt() ?: 0
    val absentCount = (session["total_absent"] as? Number)?.toInt() ?: 0

    val presentListRaw = session["present"] as? List<Map<String, String>> ?: emptyList()
    val absentListRaw = session["absent"] as? List<Map<String, String>> ?: emptyList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$date | $period",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Present: $presentCount | Absent: $absentCount",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    val summary = buildString {
                        append("Attendance Summary - $subjectId ($period on $date)\n\n")
                        append("✅ PRESENT ($presentCount):\n")
                        presentListRaw.forEach { append("- ${it["name"]} (${it["roll_no"]})\n") }
                        append("\n❌ ABSENT ($absentCount):\n")
                        absentListRaw.forEach { append("- ${it["name"]} (${it["roll_no"]})\n") }
                    }

                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Attendance Summary - $subjectId")
                        putExtra(android.content.Intent.EXTRA_TEXT, summary)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Attendance"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share text", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
