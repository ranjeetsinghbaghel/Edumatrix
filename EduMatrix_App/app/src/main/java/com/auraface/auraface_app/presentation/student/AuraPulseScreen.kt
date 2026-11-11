package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.MoodCheckInOut
import com.auraface.auraface_app.data.network.model.MoodCheckInCreate
import com.auraface.auraface_app.data.repository.PulseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuraPulseViewModel @Inject constructor(
    private val repository: PulseRepository
) : ViewModel() {
    
    var history by mutableStateOf<List<MoodCheckInOut>>(emptyList())
    var isLoading by mutableStateOf(false)
    var successMessage by mutableStateOf("")

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            isLoading = true
            try {
                val res = repository.getMyMoodHistory()
                if (res.isSuccessful) {
                    history = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Ignore for now
            } finally {
                isLoading = false
            }
        }
    }

    fun recordMood(mood: String, notes: String?) {
        viewModelScope.launch {
            isLoading = true
            try {
                val res = repository.recordDailyMood(MoodCheckInCreate(mood, notes))
                if (res.isSuccessful) {
                    val data = res.body()
                    if (data != null) {
                        successMessage = "Mood recorded! Earned ${data.xp_rewarded} XP."
                        loadHistory() // Refresh
                    }
                }
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraPulseScreen(
    navController: NavController,
    viewModel: AuraPulseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    
    val moods = listOf(
        Pair("HAPPY", Triple("\uD83D\uDE0A", "Happy", Color(0xFFFDE047))),
        Pair("FOCUSED", Triple("\uD83E\uDDE0", "Focused", Color(0xFF93C5FD))),
        Pair("NEUTRAL", Triple("\uD83D\uDE10", "Neutral", Color(0xFFD1D5DB))),
        Pair("TIRED", Triple("\uD83D\uDE2B", "Tired", Color(0xFFC4B5FD))),
        Pair("STRESSED", Triple("\uD83D\uDE29", "Stressed", Color(0xFFFCA5A5)))
    )

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage.isNotEmpty()) {
            Toast.makeText(context, viewModel.successMessage, Toast.LENGTH_SHORT).show()
            viewModel.successMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduMatrix Pulse \uD83D\uDC93", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFDB2777))
            )
        },
        containerColor = Color(0xFFFDF2F8)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("How are you feeling today?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF831843))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    moods.forEach { (key, data) ->
                        val (icon, label, color) = data
                        val isSelected = selectedMood == key
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) color else color.copy(alpha = 0.3f))
                                    .clickable { selectedMood = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF831843))
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = selectedMood != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Any thoughts? (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFDB2777),
                                focusedLabelColor = Color(0xFFDB2777)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                viewModel.recordMood(selectedMood!!, notes) 
                                selectedMood = null
                                notes = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !viewModel.isLoading
                        ) {
                            Text("Check-In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Pulse History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF831843))
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (viewModel.history.isEmpty() && !viewModel.isLoading) {
                item {
                    Text("No history yet. Record your first mood!", color = Color.Gray)
                }
            } else {
                items(viewModel.history) { entry ->
                    val color = moods.find { it.first == entry.mood }?.second?.third ?: Color.LightGray
                    val emoji = moods.find { it.first == entry.mood }?.second?.first ?: "\uD83D\uDE10"
                    val label = moods.find { it.first == entry.mood }?.second?.second ?: entry.mood

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                Text(entry.date.take(10), fontSize = 12.sp, color = Color.Gray)
                            }
                            if (!entry.notes.isNullOrBlank()) {
                                Icon(Icons.Default.Mood, contentDescription = null, tint = Color(0xFFDB2777))
                            }
                        }
                    }
                }
            }
        }
    }
}
