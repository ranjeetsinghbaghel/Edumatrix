package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherMessagesScreen(
    navController: NavController,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val notifications = viewModel.notifications
    val sentMessages = viewModel.sentMessages
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inbox", "Sent")

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }
    
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            viewModel.loadSentMessages()
        }
    }

    // Reply Dialog State
    var showReplyDialog by remember { mutableStateOf(false) }
    var replyToId by remember { mutableStateOf<Int?>(null) }
    var replyToName by remember { mutableStateOf("") }
    var replyMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Messages") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (selectedTab == 0) {
                // Inbox
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notif ->
                        Card(
                             modifier = Modifier.fillMaxWidth(),
                             colors = CardDefaults.cardColors(
                                 containerColor = if (notif.is_read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                             )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                     Text(notif.created_at, style = MaterialTheme.typography.bodySmall)

                                     if (!notif.is_read) {
                                         Badge(containerColor = MaterialTheme.colorScheme.error) {
                                             Text("New", modifier = Modifier.padding(horizontal = 4.dp))
                                         }
                                     }

                                     // Reply Logic
                                     val metadata = notif.metadata_json
                                     if (metadata != null) {
                                         val senderIdObj = metadata["sender_id"]
                                         val senderId = when(senderIdObj) {
                                             is Number -> senderIdObj.toInt()
                                             is String -> senderIdObj.toIntOrNull()
                                             else -> null
                                         }
                                         
                                         if (senderId != null) {
                                              TextButton(onClick = {
                                                  replyToId = senderId
                                                  replyToName = (metadata["sender_name"] as? String) ?: "Student"
                                                  replyMessage = ""
                                                  showReplyDialog = true
                                              }) {
                                                  Text("Reply")
                                              }
                                         }
                                     }
                                }
                            }
                        }
                    }
                    if (notifications.isEmpty()) {
                        item { Text("No messages found.", modifier = Modifier.padding(16.dp)) }
                    }
                }
            } else {
                // Sent Messages
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sentMessages) { msg ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                             Column(modifier = Modifier.padding(16.dp)) {
                                 Text("To: ${msg.target_group ?: "Unknown Group"}", style = MaterialTheme.typography.titleMedium)
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                                 Spacer(modifier = Modifier.height(8.dp))
                                 
                                 Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                     Text(msg.created_at, style = MaterialTheme.typography.bodySmall)
                                     Spacer(modifier = Modifier.weight(1f))
                                     Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                         Text("Seen: ${msg.read_count}/${msg.total_count}", modifier = Modifier.padding(4.dp))
                                     }
                                 }
                             }
                        }
                    }
                    if (sentMessages.isEmpty()) {
                        item { Text("No sent messages.", modifier = Modifier.padding(16.dp)) }
                    }
                }
            }
        }
    }

    if (showReplyDialog && replyToId != null) {
        AlertDialog(
            onDismissRequest = { showReplyDialog = false },
            title = { Text("Reply to $replyToName") },
            text = {
                OutlinedTextField(
                    value = replyMessage,
                    onValueChange = { replyMessage = it },
                    label = { Text("Your Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyMessage.isNotBlank()) {
                            viewModel.sendSectionMessage(replyMessage, replyToId) {
                                showReplyDialog = false
                            }
                        }
                    },
                    enabled = replyMessage.isNotBlank()
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplyDialog = false }) { Text("Cancel") }
            }
        )
    }
}
