package com.auraface.auraface_app.presentation.smart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auraface.auraface_app.data.remote.dto.RequiredClassesDto
import com.auraface.auraface_app.data.remote.dto.StudentRiskDto
import com.auraface.auraface_app.data.remote.dto.TrendPointDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceInsightsScreen(
    navController: androidx.navigation.NavController? = null,
    viewModel: SmartFeaturesViewModel = hiltViewModel()
) {
    val trendData by viewModel.trendData.collectAsState()
    val riskData by viewModel.riskData.collectAsState()
    val requiredClasses by viewModel.requiredClasses.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInsights()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Insights") },
                navigationIcon = {
                    TextButton(onClick = { navController?.popBackStack() }) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Trend Graph
            if (trendData.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Attendance Trend", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LineChart(data = trendData)
                    }
                }
            }

            // 2. Risk Analysis
            riskData?.let { risk ->
                RiskCard(risk)
            }

            // 3. Required Classes
            requiredClasses?.let { req ->
                if (req.required_classes > 0) {
                    RequiredClassesCard(req)
                }
            }
        }
    }
}

@Composable
fun LineChart(data: List<TrendPointDto>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (data.isEmpty()) return@Canvas

        val spacePerPoint = size.width / (data.size - 1).coerceAtLeast(1)
        val maxPercentage = 100f
        
        val points = data.mapIndexed { index, point ->
            val x = index * spacePerPoint
            val y = size.height - (point.percentage.toFloat() / maxPercentage * size.height)
            Offset(x, y)
        }

        val path = Path()
        path.moveTo(points.first().x, points.first().y)
        points.drop(1).forEach {
            path.lineTo(it.x, it.y)
        }

        drawPath(
            path = path,
            color = Color(0xFF6200EE),
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw points
        points.forEach {
            drawCircle(
                color = Color(0xFF3700B3),
                radius = 4.dp.toPx(),
                center = it
            )
        }
    }
}

@Composable
fun RiskCard(risk: StudentRiskDto) {
    val (bgColor, iconColor) = when (risk.risk_level) {
        "HIGH" -> Color(0xFFFFEBEE) to Color.Red
        "MEDIUM" -> Color(0xFFFFF3E0) to Color(0xFFFF9800)
        else -> Color(0xFFE8F5E9) to Color(0xFF4CAF50)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = risk.risk_level + " RISK",
                    style = MaterialTheme.typography.labelLarge,
                    color = iconColor
                )
                Text(
                    text = risk.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun RequiredClassesCard(req: RequiredClassesDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Target Percentage: ${req.target_percentage}%",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Attend next ${req.required_classes} classes to reach target.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
