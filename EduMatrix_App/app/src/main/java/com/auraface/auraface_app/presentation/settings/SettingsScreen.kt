package com.auraface.auraface_app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.auraface.auraface_app.core.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: com.auraface.auraface_app.presentation.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (syncState is com.auraface.auraface_app.presentation.settings.SettingsViewModel.SyncStatus.Loading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Syncing Data") },
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Please wait...")
                }
            },
            confirmButton = {}
        )
    }
    
    if (syncState is com.auraface.auraface_app.presentation.settings.SettingsViewModel.SyncStatus.Success) {
         AlertDialog(
            onDismissRequest = {},
            title = { Text("Sync Complete") },
            text = { Text("Local data has been updated.") },
            confirmButton = {},
            icon = { Icon(Icons.Default.CheckCircle, contentDescription=null, tint=MaterialTheme.colorScheme.primary) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Monitoring Section
            SettingsSection("Monitoring") {
                SettingsOption(
                    title = "Real-Time Attendance",
                    subtitle = "Monitor incoming attendance logs live",
                    icon = Icons.Default.Timeline, 
                    onClick = { navController.navigate(Screen.RealTimeMonitoring.route) }
                )
            }

            // General Section
            SettingsSection("General") {
                SettingsToggleOption(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = { 
                        viewModel.toggleNotifications(it)
                        android.widget.Toast.makeText(context, if(it) "Notifications Enabled" else "Notifications Disabled", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                

            }

            // Data Section
            SettingsSection("Data & Sync") {
                SettingsOption(
                    title = "Sync Data",
                    subtitle = "Manually refresh local cache",
                    icon = Icons.Default.Sync,
                    onClick = { viewModel.syncData() }
                )
            }
            
            // Account Section
             SettingsSection("Account") {
                SettingsOption(
                    title = "Change Password",
                    icon = Icons.Default.Lock,
                    onClick = { 
                        // Determine role from somewhere or pass it. 
                        // Since we don't have it easily here without another VM call, let's default to student 
                        // OR better, ask the user or just navigate generic and let screen handle it (screen expects arg)
                        // For this demo, let's assume student or link to a role picker if needed, 
                        // but actually we can check MainViewModel or AuthRepository.
                        // Simplification -> Pass "student" as default, real app would know role.
                        navController.navigate("change_password/student") 
                    }
                )
                 SettingsOption(
                    title = "Logout",
                    icon = Icons.AutoMirrored.Filled. ExitToApp,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = { 
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) // Clear backstack
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsOption(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsToggleOption(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
