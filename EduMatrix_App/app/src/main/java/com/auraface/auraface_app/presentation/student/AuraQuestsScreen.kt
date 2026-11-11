package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.QuestOut
import com.auraface.auraface_app.data.repository.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuraQuestsViewModel @Inject constructor(
    private val repository: QuestRepository
) : ViewModel() {
    
    var quests by mutableStateOf<List<QuestOut>>(emptyList())
    var isLoading by mutableStateOf(false)
    var claimMessage by mutableStateOf("")

    init {
        loadQuests()
    }

    private fun loadQuests() {
        viewModelScope.launch {
            isLoading = true
            try {
                val res = repository.getMyQuests()
                if (res.isSuccessful) {
                    quests = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Ignore for demo
            } finally {
                isLoading = false
            }
        }
    }

    fun claimQuest(questId: String) {
        viewModelScope.launch {
            try {
                val res = repository.claimQuest(questId)
                if (res.isSuccessful) {
                    val data = res.body()
                    if (data != null) {
                        claimMessage = data.message + if(data.level_up) " \uD83C\uDF89 LEVEL UP!" else ""
                        loadQuests() // Refresh 
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraQuestsScreen(
    navController: NavController,
    viewModel: AuraQuestsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(viewModel.claimMessage) {
        if (viewModel.claimMessage.isNotEmpty()) {
            Toast.makeText(context, viewModel.claimMessage, Toast.LENGTH_SHORT).show()
            viewModel.claimMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduMatrix Quests \uD83C\uDFC6", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF59E0B) // Amber Gold
                )
            )
        },
        containerColor = Color(0xFFFFFBEB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Daily Challenges", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Complete quests to earn XP & level up!", color = Color.White.copy(alpha=0.8f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isLoading && viewModel.quests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFF59E0B))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val claimed = viewModel.quests.filter { it.is_claimed }
                    val available = viewModel.quests.filter { !it.is_claimed }

                    if (available.isNotEmpty()) {
                        item {
                            Text("Active Quests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                        items(available) { quest ->
                            QuestCard(quest = quest, onClaim = { viewModel.claimQuest(quest.id) })
                        }
                    }

                    if (claimed.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Completed \u2705", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                        items(claimed) { quest ->
                            QuestCard(quest = quest, onClaim = {})
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun QuestCard(quest: QuestOut, onClaim: () -> Unit) {
    val iconVec = when(quest.icon) {
        "WbSunny" -> Icons.Default.WbSunny
        "Mood" -> Icons.Default.Mood
        "Psychology" -> Icons.Default.Psychology
        else -> Icons.Default.Star
    }

    val alpha = if (quest.is_claimed) 0.5f else 1f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if(quest.is_claimed) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(android.graphics.Color.parseColor(quest.bg_color.replace("0xFF", "#"))).copy(alpha = alpha),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(iconVec, contentDescription = null, modifier = Modifier.padding(12.dp), tint = Color.DarkGray)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(quest.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B).copy(alpha = alpha))
                Text(quest.description, fontSize = 12.sp, color = Color.Gray.copy(alpha = alpha))
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+${quest.xp_reward} XP", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFF59E0B).copy(alpha = alpha))
                }
            }

            if (!quest.is_claimed) {
                if (quest.is_completed) {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Claim", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                         shape = RoundedCornerShape(12.dp),
                         color = Color(0xFFF1F5F9),
                         modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("In Progress", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.Gray)
                    }
                }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "Claimed", tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
            }
        }
    }
}
