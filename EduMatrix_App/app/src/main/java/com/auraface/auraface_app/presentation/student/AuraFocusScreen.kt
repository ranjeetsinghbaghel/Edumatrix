package com.auraface.auraface_app.presentation.student

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.FocusSessionSubmit
import com.auraface.auraface_app.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {
    
    var timeRemainingSeconds by mutableStateOf(25 * 60) // Default 25 minutes
    var totalDurationSeconds by mutableStateOf(25 * 60)
    var isRunning by mutableStateOf(false)
    var isFinished by mutableStateOf(false)
    
    var showResultDialog by mutableStateOf(false)
    var xpEarned by mutableStateOf(0)
    var message by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    
    private var timerJob: Job? = null
    
    fun setDuration(minutes: Int) {
        if (!isRunning) {
            timeRemainingSeconds = minutes * 60
            totalDurationSeconds = minutes * 60
            isFinished = false
        }
    }

    fun toggleTimer() {
        if (isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        isRunning = true
        isFinished = false
        timerJob = viewModelScope.launch {
            while (timeRemainingSeconds > 0) {
                delay(1000)
                timeRemainingSeconds--
            }
            isRunning = false
            isFinished = true
            submitFocusSession()
        }
    }

    fun stopTimerEarly() {
        pauseTimer()
        // If they did at least 1 minute, give them partial credit, otherwise just reset
        val focusedSeconds = totalDurationSeconds - timeRemainingSeconds
        if (focusedSeconds >= 60) {
            submitFocusSession(focusedSeconds / 60)
        } else {
            resetTimer()
        }
    }

    private fun pauseTimer() {
        isRunning = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        timeRemainingSeconds = totalDurationSeconds
        isFinished = false
        showResultDialog = false
    }

    private fun submitFocusSession(actualMinutes: Int? = null) {
        val minutesToSubmit = actualMinutes ?: (totalDurationSeconds / 60)
        if (minutesToSubmit <= 0) return
        
        viewModelScope.launch {
            isLoading = true
            try {
                val response = quizRepository.submitFocusSession(
                    FocusSessionSubmit(duration_minutes = minutesToSubmit, subject_tag = "Deep Work")
                )
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        xpEarned = data.xp_earned
                        message = data.message
                        showResultDialog = true
                    }
                }
            } catch (e: Exception) {
                // handle error
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraFocusScreen(
    navController: NavController,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val progress = if (viewModel.totalDurationSeconds > 0) {
        viewModel.timeRemainingSeconds.toFloat() / viewModel.totalDurationSeconds.toFloat()
    } else 0f

    val formattedTime = remember(viewModel.timeRemainingSeconds) {
        val m = viewModel.timeRemainingSeconds / 60
        val s = viewModel.timeRemainingSeconds % 60
        String.format("%02d:%02d", m, s)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduMatrix Focus Flow \u23F1\uFE0F", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B)) // Dark sleek theme
            )
        },
        containerColor = Color(0xFF0F172A) // Dark background for focus
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer Selection Chips
            if (!viewModel.isRunning && viewModel.timeRemainingSeconds == viewModel.totalDurationSeconds) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        val isSelected = viewModel.totalDurationSeconds == mins * 60
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                            modifier = Modifier.clickable { viewModel.setDuration(mins) }
                        ) {
                            Text(
                                "$mins m", 
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(64.dp))
            }

            // Circular Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background track
                    drawArc(
                        color = Color(0xFF334155),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF38BDF8))
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedTime,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (viewModel.isRunning) "Deep Work" else "Paused",
                        fontSize = 18.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop/Reset Button
                if (viewModel.isRunning || viewModel.timeRemainingSeconds < viewModel.totalDurationSeconds) {
                    IconButton(
                        onClick = { viewModel.stopTimerEarly() },
                        modifier = Modifier
                            .background(Color(0xFF334155), CircleShape)
                            .size(56.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Play/Pause Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))
                        )
                        .clickable { viewModel.toggleTimer() }
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (viewModel.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        
        if (viewModel.showResultDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.resetTimer() },
                containerColor = Color(0xFF1E293B),
                title = { Text("Session Complete! \uD83C\uDF89", color = Color.White) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(viewModel.message, color = Color(0xFF94A3B8), fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("+${viewModel.xpEarned} XP", color = Color(0xFFF59E0B), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("Awesome", color = Color(0xFF0F172A))
                    }
                }
            )
        }
    }
}
