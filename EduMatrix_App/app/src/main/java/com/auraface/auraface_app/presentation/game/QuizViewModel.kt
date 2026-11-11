package com.auraface.auraface_app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.network.model.GamificationProfile
import com.auraface.auraface_app.data.network.model.QuestionResponse
import com.auraface.auraface_app.data.network.model.QuizAttemptSubmit
import com.auraface.auraface_app.data.network.model.QuizResultResponse
import com.auraface.auraface_app.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.IOException

sealed class QuizState {
    object Loading : QuizState() // Initial Loading
    data class Menu(val profile: GamificationProfile?) : QuizState() // Menu with Profile Info
    data class Error(val message: String) : QuizState()
    data class Playing(
        val questionIndex: Int,
        val totalQuestions: Int,
        val currentQuestion: QuestionResponse,
        val timeLeft: Float,
        val selectedOption: Int? = null,
        val isCorrect: Boolean? = null 
    ) : QuizState()
    data class Result(val result: QuizResultResponse) : QuizState()
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val repository: QuizRepository
) : ViewModel() {

    private val _state = MutableStateFlow<QuizState>(QuizState.Loading)
    val state = _state.asStateFlow()

    private var questions: List<QuestionResponse> = emptyList()
    private val userAnswers = mutableMapOf<Int, Int>() 
    
    private var timerJob: Job? = null
    private val timePerQuestion = 15000L // 15 seconds per question
    private var startTimeMillis: Long = 0
    private var profile: GamificationProfile? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.value = QuizState.Loading
            try {
                val response = repository.getProfile()
                if (response.isSuccessful) {
                    profile = response.body()
                }
                _state.value = QuizState.Menu(profile)
            } catch (e: Exception) {
                _state.value = QuizState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            _state.value = QuizState.Loading
            userAnswers.clear()
            try {
                val response = repository.getDailyQuiz()
                if (response.isSuccessful && response.body() != null) {
                    questions = response.body()!!
                    if (questions.isNotEmpty()) {
                        startTimeMillis = System.currentTimeMillis()
                        startQuestion(0)
                    } else {
                        _state.value = QuizState.Error("No questions available for today.")
                    }
                } else {
                    if (response.code() == 403) {
                         _state.value = QuizState.Error("You already completed today's quiz!")
                    } else {
                        _state.value = QuizState.Error("Error loading quiz: ${response.message()}")
                    }
                }
            } catch (e: IOException) {
                _state.value = QuizState.Error("Network error. Please check your connection.")
            } catch (e: Exception) {
                _state.value = QuizState.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }

    private fun startQuestion(index: Int) {
        if (index >= questions.size) {
            submitQuiz()
            return
        }

        val question = questions[index]
        _state.value = QuizState.Playing(
            questionIndex = index,
            totalQuestions = questions.size,
            currentQuestion = question,
            timeLeft = 1.0f,
            selectedOption = null
        )
        
        startTimer(index)
    }

    private fun getTimeLimit(): Long {
        val level = profile?.current_level ?: 1
        return when {
            level >= 4 -> 10000L // 10s for Elite
            level >= 3 -> 12000L // 12s for Advanced
            level >= 2 -> 13000L // 13s for Intermediate
            else -> 15000L       // 15s for Beginner
        }
    }

    private fun startTimer(index: Int) {
        timerJob?.cancel()
        val limit = getTimeLimit()
        timerJob = viewModelScope.launch {
            val start = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - start
                val progress = 1.0f - (elapsed.toFloat() / limit)
                
                val currentState = _state.value
                if (currentState !is QuizState.Playing || currentState.questionIndex != index) break

                if (progress <= 0f) {
                    // Time's up! Move to next question automatically (recording as incorrect/skipped)
                    // We record -1 or just don't add to map?
                    // Let's record -1 to indicate timeout if backend supports it, or just 0 (assuming 0 is option A)
                    // Actually, if we skip, backend sees no answer for that ID.
                    // Let's just move to next.
                    startQuestion(index + 1)
                    break
                }

                // Update UI timer only if user hasn't selected yet
                if (currentState.selectedOption == null) {
                    _state.value = currentState.copy(timeLeft = progress)
                } 
                delay(50) 
            }
        }
    }

    fun submitAnswer(optionIndex: Int) {
        val currentState = _state.value
        if (currentState is QuizState.Playing && currentState.selectedOption == null) {
            timerJob?.cancel()
            
            // Store answer
            userAnswers[currentState.currentQuestion.id] = optionIndex
            
            // Update UI to show selection
            _state.value = currentState.copy(selectedOption = optionIndex)

            // Move to next question after small delay
            viewModelScope.launch {
                delay(500) 
                startQuestion(currentState.questionIndex + 1)
            }
        }
    }

    private fun submitQuiz() {
        val currentState = _state.value
        // Show loading while submitting
        _state.value = QuizState.Loading
            
        viewModelScope.launch {
            try {
                val timeTaken = (System.currentTimeMillis() - startTimeMillis) / 1000
                val attempt = QuizAttemptSubmit(
                        answers = userAnswers,
                        time_taken_seconds = timeTaken.toInt()
                )
                
                val response = repository.submitQuiz(attempt)
                if (response.isSuccessful && response.body() != null) {
                    _state.value = QuizState.Result(response.body()!!)
                } else {
                    _state.value = QuizState.Error("Submission Failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _state.value = QuizState.Error("Submission Error: ${e.localizedMessage}")
            }
        }
    }
    
    fun restartGame() {
        loadProfile()
    }
}
