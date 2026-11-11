package com.auraface.auraface_app.presentation.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auraface.auraface_app.data.network.model.GamificationProfile
import com.auraface.auraface_app.data.network.model.QuizResultResponse
import com.auraface.auraface_app.data.network.model.QuestionResponse

@Composable
fun QuizGameScreen(
    navController: androidx.navigation.NavController,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val s = state) {
            is QuizState.Loading -> QuizLoading()
            is QuizState.Menu -> QuizMenu(s.profile, onStart = viewModel::startGame)
            is QuizState.Playing -> QuizPlaying(s, onAnswer = viewModel::submitAnswer)
            is QuizState.Result -> QuizResult(s.result, onReplay = viewModel::restartGame, onExit = { navController.popBackStack() })
            is QuizState.Error -> QuizError(s.message, onRetry = viewModel::restartGame)
        }
    }
}

@Composable
fun QuizLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun QuizError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Oops!", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun QuizMenu(profile: GamificationProfile?, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title / Logo Area
        Text(
            "Daily Quiz", 
            style = MaterialTheme.typography.displayMedium, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Profile Summary
        if (profile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LEVEL ${profile.current_level}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(profile.title, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${profile.total_xp} XP", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Streak
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥 ${profile.current_streak} Day Streak", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                    }
                }
            }
        } else {
            // Placeholder if profile failed to load but allows start
            Text("Ready to challenge yourself?", style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Start Daily Quiz", fontSize = 18.sp)
        }
    }
}

@Composable
fun QuizPlaying(state: QuizState.Playing, onAnswer: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Question ${state.questionIndex + 1}/${state.totalQuestions}", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                state.currentQuestion.difficulty, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Timer
        LinearProgressIndicator(
            progress = state.timeLeft,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(8.dp),
            color = if (state.timeLeft < 0.3f) Color.Red else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier.padding(24.dp).fillMaxSize(), 
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.currentQuestion.question_text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        if (state.currentQuestion.attachment_url != null) {
            val url = com.auraface.auraface_app.data.network.RetrofitClient.BASE_URL + state.currentQuestion.attachment_url!!.removePrefix("/")
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Attached File")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Options
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.currentQuestion.options.forEachIndexed { index, option ->
                val isSelected = state.selectedOption == index
                
                Button(
                    onClick = { if (state.selectedOption == null) onAnswer(index) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(option, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuizResult(result: QuizResultResponse, onReplay: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quiz Complete!", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Score Circle
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = result.score.toFloat() / result.total_questions,
                modifier = Modifier.size(150.dp),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${result.actual_marks?.toInt() ?: result.score}/${result.total_questions}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Actual Marks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Actual Academic Marks Badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎓", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Official Academic Marks Counted",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32)
                    )
                    val pct = result.percentage ?: ((result.score.toFloat() / result.total_questions) * 100)
                    val grade = result.grade ?: (if (pct >= 85) "A+" else if (pct >= 75) "A" else if (pct >= 60) "B" else "C")
                    Text(
                        "Score: ${result.score}/${result.total_questions} (${pct.toInt()}%) | Grade: $grade | Recorded in Gradebook",
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // XP Earned
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("XP EARNED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("+${result.xp_earned} XP", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                
                if (result.streak_bonus > 0) {
                     Text("Streak Bonus: +${result.streak_bonus}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE65100))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Level Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current Level: ${result.new_level}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = 0.5f, // You might need next_level_xp info here to calculate precise progress if not passed
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("Total XP: ${result.new_total_xp}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.End))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onExit, // Usually go back to dashboard
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Done", fontSize = 18.sp)
        }
    }
}
