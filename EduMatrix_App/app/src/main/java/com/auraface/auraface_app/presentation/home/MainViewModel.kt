package com.auraface.auraface_app.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.local.preferences.TokenManager
import com.auraface.auraface_app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.auraface.auraface_app.data.remote.dto.UserProfile
import com.auraface.auraface_app.data.repository.AuthRepository

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val tokenManager: TokenManager,
    private val authRepo: com.auraface.auraface_app.data.repository.AuthRepository
) : ViewModel() {

    var profile by mutableStateOf<UserProfile?>(null)
        private set

    val isDarkMode = preferencesRepository.isDarkMode

    init {
        loadProfile()
        syncFcmToken()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try { profile = authRepo.getProfile() } catch (e: Exception) {}
        }
    }

    private fun syncFcmToken() {
        viewModelScope.launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    viewModelScope.launch {
                        try {
                            if (tokenManager.getToken() != null) { // Only send if logged in
                                authRepo.updateFcmToken(token)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getUsername() = tokenManager.getUsername()
    fun getUserRole() = tokenManager.getRole()
    
    fun logout() {
        tokenManager.clear()
    }

    var emergencyState by mutableStateOf<EmergencyState?>(null)
        private set

    fun setEmergency(title: String, message: String) {
        emergencyState = EmergencyState(title, message)
    }

    fun clearEmergency() {
        emergencyState = null
    }
    
    var pendingChatGroupId by mutableStateOf<String?>(null)
        private set
        
    fun setPendingChatGroup(groupId: String) {
        pendingChatGroupId = groupId
    }
    
    fun clearPendingChatGroup() {
        pendingChatGroupId = null
    }
}

data class EmergencyState(
    val title: String,
    val message: String
)
