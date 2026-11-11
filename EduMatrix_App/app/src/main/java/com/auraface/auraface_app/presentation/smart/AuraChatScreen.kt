package com.auraface.auraface_app.presentation.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraChatScreen(
    onNavigateBack: () -> Unit,
    navController: androidx.navigation.NavController? = null,
    viewModel: SmartFeaturesViewModel = hiltViewModel()
) {
    val messages by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.chatLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AI", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ask EduMatrix")
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = { 
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                enabled = !isLoading
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg, onAction = { action ->
                        when(action) {
                            "NAV_INSIGHTS", "NAVIGATE_INSIGHTS" -> navController?.navigate(com.auraface.auraface_app.core.navigation.Screen.SmartInsights.route)
                            "NAV_NOTICES", "NAVIGATE_NOTICES" -> navController?.navigate(com.auraface.auraface_app.core.navigation.Screen.SmartNotices.route)
                            "NAVIGATE_ATTENDANCE" -> navController?.navigate(com.auraface.auraface_app.core.navigation.Screen.StudentHistory.route)
                            "NAVIGATE_TIMETABLE" -> navController?.navigate("student_schedule")
                            "NAVIGATE_ACADEMIC_CALENDAR" -> navController?.navigate("academic_calendar")
                            "NAVIGATE_STUDY_PLANNER" -> navController?.navigate("study_planner")
                        }
                    })
                }
                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onAction: (String) -> Unit = {}) {
    val align = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else Color.White
    val textColor = if (message.isUser) Color.White else Color.Black
    val shape = if (message.isUser) 
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) 
    else 
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = align
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bgColor,
                shape = shape,
                shadowElevation = 2.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (!message.isUser && message.action != null) {
                // Action Chip
                val label = when(message.action) {
                    "NAV_INSIGHTS", "NAVIGATE_INSIGHTS" -> "View Insights"
                    "NAV_NOTICES", "NAVIGATE_NOTICES" -> "Open Notices"
                    "NAVIGATE_ATTENDANCE" -> "View Attendance"
                    "NAVIGATE_TIMETABLE" -> "View Timetable"
                    "NAVIGATE_ACADEMIC_CALENDAR" -> "View Calendar"
                    "NAVIGATE_STUDY_PLANNER" -> "AI Planner"
                    else -> "Details"
                }
                AssistChip(
                    onClick = { onAction(message.action) },
                    label = { Text(label) },
                    modifier = Modifier.padding(top = 4.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.White, CircleShape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text("EduMatrix is thinking...", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about attendance...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}
