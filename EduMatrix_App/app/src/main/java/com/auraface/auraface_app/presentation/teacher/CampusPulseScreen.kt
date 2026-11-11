package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.DailyPulseInsight
import com.auraface.auraface_app.data.network.model.PulseDashboardOut
import com.auraface.auraface_app.data.repository.PulseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CampusPulseViewModel @Inject constructor(
    private val repository: PulseRepository
) : ViewModel() {
    
    var dashboard by mutableStateOf<PulseDashboardOut?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf("")

    init {
        loadInsights()
    }

    private fun loadInsights() {
        viewModelScope.launch {
            isLoading = true
            try {
                // For now fetching global insights (admin view). We could pass department for teachers.
                val res = repository.getCampusInsights()
                if (res.isSuccessful) {
                    dashboard = res.body()
                } else {
                    error = "Failed to load insights."
                }
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusPulseScreen(
    navController: NavController,
    viewModel: CampusPulseViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Pulse \uD83D\uDCCA", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4F46E5))
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4F46E5))
            }
        } else if (viewModel.dashboard == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available for today.", color = Color.Gray)
            }
        } else {
            val data = viewModel.dashboard!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Stats
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Check-ins Today", color = Color.White.copy(alpha=0.8f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(data.total_students.toString(), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color.White.copy(alpha=0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mood, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Dominant Mood: ${data.dominant_mood}", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Mood Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }

                // Progress Bars List
                items(data.insights.size) { index ->
                    val insight = data.insights[index]
                    val color = when(insight.mood) {
                        "HAPPY" -> Color(0xFFF59E0B)
                        "FOCUSED" -> Color(0xFF3B82F6)
                        "NEUTRAL" -> Color(0xFF94A3B8)
                        "TIRED" -> Color(0xFF8B5CF6)
                        "STRESSED" -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }
                    val emoji = when(insight.mood) {
                        "HAPPY" -> "\uD83D\uDE0A"
                        "FOCUSED" -> "\uD83E\uDDE0"
                        "NEUTRAL" -> "\uD83D\uDE10"
                        "TIRED" -> "\uD83D\uDE2B"
                        "STRESSED" -> "\uD83D\uDE29"
                        else -> "\uD83D\uDE10"
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(insight.mood, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                            }
                            Text("${insight.percentage.toInt()}% (${insight.count})", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = insight.percentage / 100f)
                                    .height(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
