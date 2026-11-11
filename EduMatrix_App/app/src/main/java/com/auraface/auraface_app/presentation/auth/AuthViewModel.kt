package com.auraface.auraface_app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.core.NetworkErrorMapper
import com.auraface.auraface_app.data.local.preferences.TokenManager
import com.auraface.auraface_app.data.network.api.AuthApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String, selectedRole: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = authApi.login(username, password)
                
                // Verify Role
                if (!response.role.equals(selectedRole, ignoreCase = true)) {
                    _loginState.value = LoginState.Error("Access Denied: Account is not registered as ${selectedRole.lowercase().replaceFirstChar { it.uppercase() }}")
                    return@launch
                }

                // Save session info
                tokenManager.saveToken(response.access_token)
                tokenManager.saveRole(response.role)
                tokenManager.saveUsername(response.username)
                tokenManager.saveFlags(response.is_hod ?: false, response.is_class_teacher ?: false)

                _loginState.value = LoginState.Success(
                    role = response.role
                )

            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    _loginState.value = LoginState.Error("Invalid username or password")
                } else {
                    _loginState.value = LoginState.Error("Server error: ${e.code()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(NetworkErrorMapper.toUserMessage(e))
            }
        }
    }

    // --- Forgot Password Logic ---
    var forgotPasswordState by androidx.compose.runtime.mutableStateOf<ForgotPasswordState>(ForgotPasswordState.Idle)
        private set

    fun requestOtp(username: String) {
        viewModelScope.launch {
            forgotPasswordState = ForgotPasswordState.Loading
            try {
                authApi.forgotPassword(mapOf("username" to username))
                forgotPasswordState = ForgotPasswordState.OtpSent(username)
            } catch (e: Exception) {
                forgotPasswordState = ForgotPasswordState.Error(NetworkErrorMapper.toUserMessage(e))
            }
        }
    }

    fun verifyOtp(username: String, otp: String) {
        viewModelScope.launch {
            forgotPasswordState = ForgotPasswordState.Loading
            try {
                authApi.verifyOtp(mapOf("username" to username, "otp" to otp))
                forgotPasswordState = ForgotPasswordState.OtpVerified(username, otp)
            } catch (e: Exception) {
                forgotPasswordState = ForgotPasswordState.Error("Invalid OTP")
            }
        }
    }

    fun resetPassword(username: String, otp: String, newPassword: String) {
        viewModelScope.launch {
            forgotPasswordState = ForgotPasswordState.Loading
            try {
                authApi.resetPassword(mapOf("username" to username, "otp" to otp, "new_password" to newPassword))
                forgotPasswordState = ForgotPasswordState.Success
            } catch (e: Exception) {
                forgotPasswordState = ForgotPasswordState.Error("Failed to reset password")
            }
        }
    }

    fun resetForgotPasswordState() {
        forgotPasswordState = ForgotPasswordState.Idle
    }
}

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class OtpSent(val username: String) : ForgotPasswordState()
    data class OtpVerified(val username: String, val otp: String) : ForgotPasswordState()
    object Success : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}
