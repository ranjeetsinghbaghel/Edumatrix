package com.auraface.auraface_app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.auraface.auraface_app.core.navigation.AppNavGraph
import com.auraface.auraface_app.presentation.home.MainViewModel
import com.auraface.auraface_app.ui.theme.AuraFaceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission Granted
        } else {
            // Permission Denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Notification Permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        handleIntent(intent)

        setContent {
            // Restore the state connection when ready
            val isDarkMode by mainViewModel.isDarkMode.collectAsState(initial = false)
            val emergencyState = mainViewModel.emergencyState

            AuraFaceTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()
                    
                    if (emergencyState != null) {
                        EmergencyAlertDialog(
                            title = emergencyState.title,
                            message = emergencyState.message,
                            onDismiss = { mainViewModel.clearEmergency() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("is_emergency", false) == true) {
            val title = intent.getStringExtra("title") ?: "Emergency Alert"
            val body = intent.getStringExtra("body") ?: "Please check app."
            mainViewModel.setEmergency(title, body)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        }
        
        val chatGroupId = intent?.getStringExtra("open_chat_group_id")
        if (chatGroupId != null) {
            mainViewModel.setPendingChatGroup(chatGroupId)
            // Clear the intent extra so we don't open it again on rotation
            intent.removeExtra("open_chat_group_id")
        }
    }

    private val emergencyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            val title = intent?.getStringExtra("title") ?: return
            val body = intent.getStringExtra("body") ?: return
            mainViewModel.setEmergency(title, body)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = android.content.IntentFilter("com.auraface.auraface_app.EMERGENCY_ALERT")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(emergencyReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(emergencyReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(emergencyReceiver)
    }

    @androidx.compose.runtime.Composable
    fun EmergencyAlertDialog(title: String, message: String, onDismiss: () -> Unit) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { androidx.compose.material3.Text(text = title, color = androidx.compose.material3.MaterialTheme.colorScheme.error) },
            text = { androidx.compose.material3.Text(text = message) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = onDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error)
                ) {
                    androidx.compose.material3.Text("Acknowledge")
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
            titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
            textContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
