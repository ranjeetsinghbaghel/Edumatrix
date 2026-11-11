package com.auraface.auraface_app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.repository.AuthRepository
import com.auraface.auraface_app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    // Delegate to repository
    val darkModeEnabled = preferencesRepository.isDarkMode
    
    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState = _syncState.asStateFlow()

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        // In a real app, save to DataStore/SharedPreferences here
    }

    fun toggleDarkMode(enabled: Boolean) {
        preferencesRepository.setDarkMode(enabled)
        // In a real app, save to DataStore/SharedPreferences here
    }
    
    fun syncData() {
        viewModelScope.launch {
            _syncState.value = SyncStatus.Loading
            delay(2000) // Simulate network delay
            // Here you would call repository functions to refresh local cache
            _syncState.value = SyncStatus.Success
            delay(1500) // Show success for a moment
            _syncState.value = SyncStatus.Idle
        }
    }
    
    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Loading : SyncStatus()
        object Success : SyncStatus()
        object Error : SyncStatus()
    }
}
