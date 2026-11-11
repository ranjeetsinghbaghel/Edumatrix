package com.auraface.auraface_app.presentation.auth

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.core.navigation.Screen
import com.auraface.auraface_app.presentation.auth.AuthViewModel
import com.auraface.auraface_app.presentation.auth.LoginState
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {

        
            navController.navigate(Screen.MainDashboard.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    var showForgotDialog by remember { mutableStateOf(false) }

    LoginUI(
        loginState = loginState,
        onLoginClick = { username, password, role ->
            viewModel.login(username, password, role)
        },
        onForgotPasswordClick = { showForgotDialog = true }
    )

    if (showForgotDialog) {
        ForgotPasswordDialog(
            viewModel = viewModel,
            onDismiss = {
                showForgotDialog = false
                viewModel.resetForgotPasswordState()
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val state = viewModel.forgotPasswordState
    var usernameInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forgot Password") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                when (state) {
                    is ForgotPasswordState.Idle, is ForgotPasswordState.Error -> {
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Enter Username") },
                            isError = state is ForgotPasswordState.Error
                        )
                        if (state is ForgotPasswordState.Error) {
                            Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is ForgotPasswordState.OtpSent -> {
                        Text("OTP sent to email (Check Console/Logs)")
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { otpInput = it },
                            label = { Text("Enter 6-digit OTP") }
                        )
                    }
                    is ForgotPasswordState.OtpVerified -> {
                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = { newPasswordInput = it },
                            label = { Text("New Password") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                    }
                    is ForgotPasswordState.Success -> {
                        Text("Password Reset Successfully!", color = MaterialTheme.colorScheme.primary)
                    }
                    else -> { CircularProgressIndicator() }
                }
            }
        },
        confirmButton = {
            if (state !is ForgotPasswordState.Success) {
                Button(onClick = {
                    when (state) {
                        is ForgotPasswordState.Idle, is ForgotPasswordState.Error -> {
                            if (usernameInput.isNotEmpty()) viewModel.requestOtp(usernameInput)
                        }
                        is ForgotPasswordState.OtpSent -> {
                            if (otpInput.isNotEmpty()) viewModel.verifyOtp(state.username, otpInput)
                        }
                        is ForgotPasswordState.OtpVerified -> {
                            if (newPasswordInput.isNotEmpty()) viewModel.resetPassword(state.username, state.otp, newPasswordInput)
                        }
                        else -> {}
                    }
                }) {
                    Text(
                        when (state) {
                            is ForgotPasswordState.Idle, is ForgotPasswordState.Error -> "Send OTP"
                            is ForgotPasswordState.OtpSent -> "Verify OTP"
                            is ForgotPasswordState.OtpVerified -> "Reset Password"
                            else -> "Processing..."
                        }
                    )
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
