@file:OptIn(ExperimentalMaterial3Api::class)


package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun AdminReportsScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val reportData = viewModel.reportData

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Reports") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Yearly Trend Graph
            val trend = reportData?.yearlyTrends?.firstOrNull()
            if (trend != null) {
                Text(
                    "Attendance Trend ${trend.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    AttendanceLineChart(trend.monthlyData)
                }
            }

            // 2. Subject Performance
            Text(
                "Subject Performance Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            reportData?.subjectPerformance?.forEach { subject ->
                SubjectReportCard(subject)
            } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun AttendanceLineChart(data: List<com.auraface.auraface_app.data.network.model.MonthlyTrend>) {
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 32.dp)) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)
        val maxVal = 100f

        val path = Path()
        data.forEachIndexed { index, point ->
            val x = index * spacing
            val y = height - (point.averageAttendance / maxVal) * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            
            drawCircle(color = Color(0xFF6200EE), radius = 8f, center = Offset(x, y))
        }

        drawPath(
            path = path,
            color = Color(0xFF6200EE),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun SubjectReportCard(subject: com.auraface.auraface_app.data.network.model.SubjectReport) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.subjectName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${subject.totalClasses} Classes conducted", fontSize = 12.sp, color = Color.Gray)
            }
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { subject.averageAttendance / 100f },
                    modifier = Modifier.size(50.dp),
                    color = if (subject.averageAttendance > 75) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text("${subject.averageAttendance.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
