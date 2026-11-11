package com.auraface.auraface_app.presentation.auth

import androidx.lifecycle.ViewModel
import com.auraface.auraface_app.data.local.preferences.TokenManager
import com.auraface.auraface_app.core.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    suspend fun getStartRoute(): String {
        return if (tokenManager.getToken() != null) {
            Screen.MainDashboard.route   // ✅ ALWAYS MainDashboard
        } else {
            Screen.Login.route
        }
    }
}

