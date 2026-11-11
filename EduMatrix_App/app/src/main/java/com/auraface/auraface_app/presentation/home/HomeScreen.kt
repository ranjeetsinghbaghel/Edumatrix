package com.auraface.auraface_app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.core.navigation.Screen
import com.auraface.auraface_app.presentation.auth.AuthViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Welcome to EduMatrix")

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                navController.navigate(Screen.Camera.route)
            }) {
                Text("Mark Attendance")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                onLogout()
            }) {
                Text("Logout")
            }
        }
    }
}
