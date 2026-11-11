package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeMonitoringScreen(
    navController: NavController
) {
    // Simulated Live Data (replace with ViewModel data later)
    var logs by remember { mutableStateOf<List<LiveLog>>(emptyList()) }
    var isMonitoring by remember { mutableStateOf(true) }

    // Simulation effect
    LaunchedEffect(isMonitoring) {
        if (isMonitoring) {
            while (true) {
                delay(3000) // update every 3 seconds
                // Simulate new log
                val newLog = generateRandomLog()
                logs = (listOf(newLog) + logs).take(50) // Keep last 50
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Real-Time Monitoring", fontWeight = FontWeight.Bold)
                        Text(
                            if(isMonitoring) "● Live Updating" else "○ Paused", 
                            style = MaterialTheme.typography.labelSmall,
                            color = if(isMonitoring) Color.Green else Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isMonitoring = !isMonitoring }) {
                        Icon(
                            if (isMonitoring) Icons.Default.Refresh else Icons.Default.Refresh, // Can switch icon
                            contentDescription = "Toggle",
                            tint = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface 
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Waiting for attendance activity...", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(logs) { log ->
                        LiveLogItem(log)
                    }
                }
            }
        }
    }
}

data class LiveLog(
    val studentName: String,
    val subject: String,
    val time: String,
    val status: String = "Present",
    val id: String = UUID.randomUUID().toString()
)

fun generateRandomLog(): LiveLog {
    val names = listOf("Ranjeet Singh", "Arun Kumar", "Gayatri Dhal", "Suresh Raina", "Virat Kohli")
    val subjects = listOf("Mobile App Dev", "Cloud Computing", "Cyber Security", "AI/ML")
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    return LiveLog(
        studentName = names.random(),
        subject = subjects.random(),
        time = sdf.format(Date())
    )
}

@Composable
fun LiveLogItem(log: LiveLog) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    log.studentName.take(1),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(log.studentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Subject: ${log.subject}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(log.time, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    log.status, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
