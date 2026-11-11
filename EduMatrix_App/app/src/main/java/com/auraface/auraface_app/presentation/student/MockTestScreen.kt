@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.api.QuizApi
import com.auraface.auraface_app.data.network.model.QuestionResponse
import com.auraface.auraface_app.data.remote.dto.MockTestResultDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MockTestViewModel @Inject constructor(
    private val quizApi: QuizApi
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _questions = MutableStateFlow<List<QuestionResponse>>(emptyList())
    val questions: StateFlow<List<QuestionResponse>> = _questions

    private val _result = MutableStateFlow<MockTestResultDto?>(null)
    val result: StateFlow<MockTestResultDto?> = _result

    fun loadQuestions(category: String, onError: (String) -> Unit) {
        _loading.value = true
        _result.value = null
        _questions.value = emptyList()
        viewModelScope.launch {
            try {
                val res = quizApi.getMockTestQuestions(category)
                if (res.isSuccessful && res.body() != null) {
                    _questions.value = res.body()!!
                } else {
                    onError("Failed to fetch questions")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to connect to server")
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitMockTest(
        category: String,
        answers: Map<Int, Int>,
        timeTakenSeconds: Int,
        onError: (String) -> Unit
    ) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val reqBody: Map<String, Any> = mapOf(
                    "category" to category,
                    "answers" to answers.mapKeys { it.key.toString() },
                    "time_taken_seconds" to timeTakenSeconds
                )
                val res = quizApi.submitMockTest(reqBody)
                if (res.isSuccessful && res.body() != null) {
                    _result.value = res.body()!!
                } else {
                    onError("Submission failed: ${res.message()}")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Submission failed")
            } finally {
                _loading.value = false
            }
        }
    }

    fun reset() {
        _questions.value = emptyList()
        _result.value = null
    }
}

@Composable
fun MockTestScreen(
    navController: NavController,
    viewModel: MockTestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val result by viewModel.result.collectAsState()

    var testCategory by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("GENERAL") }
    var selectedDurationMins by remember { mutableStateOf(5) }
    var currentQuestionIdx by remember { mutableStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() } // questionId -> selectedOptionIdx

    // Timer state
    var timeLeftSeconds by remember { mutableStateOf(300) }
    var timeTakenSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(questions, timeLeftSeconds) {
        if (questions.isNotEmpty() && timeLeftSeconds > 0 && result == null) {
            delay(1000)
            timeLeftSeconds--
            timeTakenSeconds++
        } else if (questions.isNotEmpty() && timeLeftSeconds == 0 && result == null) {
            // Auto submit
            Toast.makeText(context, "Time is up! Auto-submitting your test.", Toast.LENGTH_LONG).show()
            val answersList = questions.associate { it.id to (userAnswers[it.id] ?: -1) }
            viewModel.submitMockTest(testCategory, answersList, timeTakenSeconds) { err ->
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Mock Test Center", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4F46E5))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            if (loading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5), strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading assessment module...", fontWeight = FontWeight.Bold)
                }
            } else if (result != null) {
                MockTestResultView(
                    result = result!!,
                    questions = questions,
                    onRestart = {
                        viewModel.reset()
                        testCategory = ""
                        currentQuestionIdx = 0
                        userAnswers.clear()
                    }
                )
            } else if (questions.isEmpty()) {
                // Settings Selection Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Practice Mock Exam",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Practice high-quality subject modules under standardized time conditions.",
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Select Subject Category", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val categories = listOf("SUBJECT", "APTITUDE", "GENERAL")
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isSel = cat == categorySelection
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { categorySelection = cat }
                                    ) {
                                        Text(
                                            text = cat,
                                            color = if (isSel) Color.White else Color(0xFF64748B),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Select Time Duration", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(8.dp))

                            val durations = listOf(5, 10, 15)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                durations.forEach { mins ->
                                    val isSel = mins == selectedDurationMins
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedDurationMins = mins }
                                    ) {
                                        Text(
                                            text = "$mins Mins",
                                            color = if (isSel) Color.White else Color(0xFF64748B),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            testCategory = categorySelection
                            timeLeftSeconds = selectedDurationMins * 60
                            timeTakenSeconds = 0
                            viewModel.loadQuestions(categorySelection) { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Launch Assessment Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                // Active Test View
                val currentQ = questions[currentQuestionIdx]
                val selectedOption = userAnswers[currentQ.id]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Timer and Progress Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val mins = timeLeftSeconds / 60
                        val secs = timeLeftSeconds % 60
                        val isLowTime = timeLeftSeconds < 60
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isLowTime) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                fontWeight = FontWeight.Black,
                                color = if (isLowTime) Color(0xFFEF4444) else Color(0xFF1E293B),
                                fontSize = 18.sp
                            )
                        }

                        Text(
                            text = "${currentQuestionIdx + 1} of ${questions.size}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Question Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxSize()
                        ) {
                            Text(
                                text = currentQ.question_text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                lineHeight = 24.sp
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Options
                            val opts = currentQ.options ?: emptyList()
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(opts) { idx, option ->
                                    val isSelected = selectedOption == idx
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { userAnswers[currentQ.id] = idx }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { userAnswers[currentQ.id] = idx },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = option,
                                                color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF475569),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation indexing quick-checker
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(72.dp)
                    ) {
                        gridItemsIndexed(questions) { idx, q ->
                            val answered = userAnswers.containsKey(q.id)
                            val active = currentQuestionIdx == idx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) Color(0xFF4F46E5) 
                                        else if (answered) Color(0xFFD1FAE5) 
                                        else Color(0xFFE2E8F0),
                                modifier = Modifier.clickable { currentQuestionIdx = idx }
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = if (active) Color.White 
                                            else if (answered) Color(0xFF065F46) 
                                            else Color(0xFF475569),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Navigation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentQuestionIdx > 0) {
                            Button(
                                onClick = { currentQuestionIdx-- },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Prev", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                if (currentQuestionIdx < questions.size - 1) {
                                    currentQuestionIdx++
                                } else {
                                    // Submit
                                    val answersMap = questions.associate { it.id to (userAnswers[it.id] ?: -1) }
                                    viewModel.submitMockTest(testCategory, answersMap, timeTakenSeconds) { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (currentQuestionIdx < questions.size - 1) "Next" else "Finish Exam",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MockTestResultView(
    result: MockTestResultDto,
    questions: List<QuestionResponse>,
    onRestart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Mock Test Scorecard", fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${result.score}",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "/${result.total_questions}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Time Taken", fontSize = 12.sp, color = Color.White.copy(0.7f))
                                val mins = result.time_taken_seconds / 60
                                val secs = result.time_taken_seconds % 60
                                Text(String.format("%02d:%02d", mins, secs), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("XP Awarded", fontSize = 12.sp, color = Color.White.copy(0.7f))
                                Text("+${result.xp_earned} XP", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Incorrect Answer Review & Explanations",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF334155),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(questions) { q ->
            val correctIdx = result.correct_answers[q.id.toString()] ?: 0
            val explanation = result.explanations[q.id.toString()] ?: "No explanation supplied."
            val opts = q.options ?: emptyList()
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = q.question_text,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Correct Answer: ${opts.getOrNull(correctIdx) ?: "None"}",
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Explanation:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = explanation,
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Another Mock Test", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
