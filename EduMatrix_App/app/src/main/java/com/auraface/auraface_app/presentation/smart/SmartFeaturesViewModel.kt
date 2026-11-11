package com.auraface.auraface_app.presentation.smart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.remote.dto.*
import com.auraface.auraface_app.data.repository.SmartFeaturesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartFeaturesViewModel @Inject constructor(
    private val repository: SmartFeaturesRepository,
    private val tokenManager: com.auraface.auraface_app.data.local.preferences.TokenManager
) : ViewModel() {

    val isAdmin = tokenManager.getRole() == "admin"

    // Notices
    private val _notices = MutableStateFlow<List<NoticeDto>>(emptyList())
    val notices = _notices.asStateFlow()

    private val _noticeReaders = MutableStateFlow<List<NoticeReaderDto>>(emptyList())
    val noticeReaders = _noticeReaders.asStateFlow()

    private val _isLoadingNotices = MutableStateFlow(false)
    val isLoadingNotices = _isLoadingNotices.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Insights
    private val _trendData = MutableStateFlow<List<TrendPointDto>>(emptyList())
    val trendData = _trendData.asStateFlow()

    private val _riskData = MutableStateFlow<StudentRiskDto?>(null)
    val riskData = _riskData.asStateFlow()

    private val _requiredClasses = MutableStateFlow<RequiredClassesDto?>(null)
    val requiredClasses = _requiredClasses.asStateFlow()

    // Chat
    private val _chatHistory = MutableStateFlow(
        listOf(ChatMessage(content = "Hi there! I'm EduMatrix AI. How can I help you with your attendance today?", isUser = false))
    )
    val chatHistory = _chatHistory.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading = _chatLoading.asStateFlow()

    fun loadNoticeReaders(noticeId: Int) {
        viewModelScope.launch {
            try {
                _noticeReaders.value = repository.getNoticeReaders(noticeId)
            } catch (e: Exception) {
                // Handle error or just show empty
                _noticeReaders.value = emptyList()
            }
        }
    }

    fun loadNotices() {
        viewModelScope.launch {
            _isLoadingNotices.value = true
            _errorMessage.value = null
            try {
                _notices.value = repository.getNoticeList()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load notices: ${e.message}"
            } finally {
                _isLoadingNotices.value = false
            }
        }
    }

    fun markNoticeRead(id: Int) {
        viewModelScope.launch {
            try {
                repository.markNoticeRead(id)
                // Update local state to reflect read status
                _notices.value = _notices.value.map {
                    if (it.id == id) it.copy(is_read = true) else it
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadInsights() {
        viewModelScope.launch {
            try {
                // Load sequentially or use async/await
                _trendData.value = repository.getAttendanceTrend()
                _riskData.value = repository.getStudentRisk()
                _requiredClasses.value = repository.getRequiredClasses()
            } catch (e: Exception) {
                 // Handle error
            }
        }
    }

    fun sendMessage(messageContent: String) {
        if (messageContent.isBlank()) return

        val userMsg = ChatMessage(content = messageContent, isUser = true)
        _chatHistory.value += userMsg
        _chatLoading.value = true

        viewModelScope.launch {
            try {
                val response = repository.askAura(messageContent)
                val aiMsg = ChatMessage(
                    content = response.reply, 
                    isUser = false, 
                    action = response.action
                )
                _chatHistory.value += aiMsg
            } catch (e: Exception) {
                _chatHistory.value += ChatMessage(content = "Sorry, I'm having trouble connecting right now.", isUser = false)
            } finally {
                _chatLoading.value = false
            }
        }
    }
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val action: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
