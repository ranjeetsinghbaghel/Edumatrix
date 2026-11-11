package com.auraface.auraface_app.presentation.smart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auraface.auraface_app.data.remote.dto.NoticeDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartNoticeBoardScreen(
    navController: androidx.navigation.NavController? = null,
    viewModel: SmartFeaturesViewModel = hiltViewModel()
) {
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoadingNotices.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val isAdmin = viewModel.isAdmin
    var showReadersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadNotices()
    }
    
    if (showReadersDialog) {
        val readers by viewModel.noticeReaders.collectAsState()
        AlertDialog(
            onDismissRequest = { showReadersDialog = false },
            title = { Text("Viewed By: ${readers.size}") },
            text = {
                if (readers.isEmpty()) {
                    Text("No one has viewed this notice yet.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(readers) { reader ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(reader.name, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(reader.role, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(formatDate(reader.read_at), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Divider(modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReadersDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notice Board", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { navController?.popBackStack() }) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            var selectedTab by remember { mutableStateOf(0) }
            var searchQuery by remember { mutableStateOf("") }
            val tabs = listOf("All", "Unread", "High Priority")
            
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notices...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            val filteredNotices = notices.filter {
                (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.message.contains(searchQuery, ignoreCase = true))
            }.let { list ->
                when(selectedTab) {
                    0 -> list
                    1 -> list.filter { !it.is_read }
                    2 -> list.filter { it.priority == "HIGH" }
                    else -> list
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    errorMessage != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadNotices() }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Retry")
                            }
                        }
                    }
                    filteredNotices.isEmpty() -> {
                        Text(
                            if (notices.isEmpty()) "No notices found." else "No notices in this category.",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredNotices) { notice ->
                                NoticeCard(
                                    notice = notice,
                                    onRead = { viewModel.markNoticeRead(notice.id) },
                                    isAdmin = isAdmin,
                                    onViewReaders = {
                                        viewModel.loadNoticeReaders(notice.id)
                                        showReadersDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeCard(notice: NoticeDto, onRead: () -> Unit, isAdmin: Boolean = false, onViewReaders: () -> Unit = {}) {
    val priorityColor = when (notice.priority) {
        "HIGH" -> Color.Red
        "MEDIUM" -> Color(0xFFFFA000) // Amber
        else -> Color(0xFF4CAF50) // Green
    }
    
    val containerColor = if (notice.is_read) 
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    else 
        MaterialTheme.colorScheme.surface

    Card(
        onClick = onRead,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notice.is_read) 0.dp else 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Priority Badge
                Surface(
                    color = priorityColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, priorityColor)
                ) {
                    Text(
                        text = notice.priority,
                        color = priorityColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (!notice.is_read) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) { 
                        Text("NEW", modifier = Modifier.padding(2.dp), color = Color.White) 
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (notice.is_read) FontWeight.Normal else FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = notice.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (notice.is_read) 2 else 10
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${notice.view_count} views",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatDate(notice.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onViewReaders,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Who Viewed This?", fontSize = 12.sp)
                }
            }
        }
    }
}

fun formatDate(isoString: String): String {
    return try {
        // Simple manual parsing or use java.time logic if API versions allow (minSdk 26+)
        isoString.substring(0, 10) 
    } catch (e: Exception) {
        isoString
    }
}
