package com.auraface.auraface_app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.navigation.NavController
import com.auraface.auraface_app.core.Constants
import com.auraface.auraface_app.data.network.model.ChatGroup
import com.auraface.auraface_app.data.network.model.ChatMessageDto
import androidx.core.net.toFile
import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatMainScreen(
    navController: NavController,
    isGroupMode: Boolean = true,
    autoOpenGroupId: String? = null,
    autoOpenGroupName: String? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentGroup = viewModel.currentGroup
    val context = LocalContext.current
    
    LaunchedEffect(viewModel.groups.size, autoOpenGroupId, viewModel.currentUserId) {
        if (autoOpenGroupId != null && currentGroup == null && viewModel.currentUserId != 0) {
            val groupToOpen = viewModel.groups.find { it.group_id == autoOpenGroupId }
            if (groupToOpen != null) {
                viewModel.enterChat(groupToOpen)
            } else if (autoOpenGroupId.startsWith("DM_") && viewModel.groups.isNotEmpty()) {
                val newGroup = ChatGroup(
                    group_id = autoOpenGroupId,
                    name = autoOpenGroupName ?: "Chat",
                    department = null, year = null, section = null,
                    last_message = null, unread_count = 0
                )
                viewModel.groups.add(0, newGroup)
                viewModel.enterChat(newGroup)
            }
        }
    }
    
    if (currentGroup == null) {
        val filteredGroups = if (isGroupMode) {
            viewModel.groups.filter { !it.group_id.startsWith("DM_") }
        } else {
            viewModel.groups.filter { it.group_id.startsWith("DM_") }
        }

        // Show List of Groups
        GroupListScreen(
            groups = filteredGroups,
            isGroupMode = isGroupMode,
            onGroupClick = { group -> viewModel.enterChat(group) },
            onBack = { navController.popBackStack() },
            isLoading = viewModel.isLoading,
            currentUserId = viewModel.currentUserId,
            contacts = viewModel.contacts,
            onContactClick = { contact -> viewModel.startDM(contact) }
        )
    } else {
        // Show Chat Room
        ChatRoomScreen(
            group = currentGroup,
            messages = viewModel.messages,
            currentUserId = viewModel.currentUserId,
            messageText = viewModel.newMessageText,
            onMessageChange = { viewModel.newMessageText = it },
            onSend = { viewModel.sendMessage() },
            onSendFile = { uri, mimeType ->
                val file = getFileFromUri(context, uri)
                if (file != null) {
                    viewModel.sendFile(file, mimeType)
                }
            },
            onBack = { viewModel.leaveChat() },
            isEditing = viewModel.editingMessageId != null,
            onCancelEdit = { viewModel.cancelEditing() },
            onBeginEdit = { viewModel.startEditing(it) },
            onDelete = { viewModel.deleteMessage(it) },
            onDeleteGroup = {
                viewModel.deleteGroup(currentGroup.group_id) {
                    navController.popBackStack()
                }
            },
            onUpdateGroupName = { newName ->
                viewModel.updateGroupName(currentGroup.group_id, newName) {
                    // updated successfully
                }
            },
            onUploadGroupImage = { file ->
                viewModel.uploadGroupImage(currentGroup.group_id, file) {
                    // updated successfully
                }
            },
            onDuelRequest = { amount, category ->
                // Determine target user from DM group ID
                val parts = currentGroup.group_id.split("_")
                val u1 = parts.getOrNull(1)?.toIntOrNull() ?: viewModel.currentUserId
                val u2 = parts.getOrNull(2)?.toIntOrNull() ?: viewModel.currentUserId
                val targetId = if (u1 == viewModel.currentUserId) u2 else u1
                
                viewModel.sendDuelXP(targetId, amount, category, 
                    onSuccess = { 
                        // Handled natively by WS 
                    }, 
                    onError = { 
                        // Should show snackbar practically
                    }
                )
            }
        )
    }
}

fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "temp_file_${System.currentTimeMillis()}")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    groups: List<ChatGroup>,
    isGroupMode: Boolean,
    onGroupClick: (ChatGroup) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    currentUserId: Int,
    contacts: List<com.auraface.auraface_app.data.network.model.ChatContact>,
    onContactClick: (com.auraface.auraface_app.data.network.model.ChatContact) -> Unit
) {
    var showContactsDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGroupMode) "Class Group Chats" else "My Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isGroupMode) {
                FloatingActionButton(
                    onClick = { showContactsDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New Chat")
                }
            } else {
                FloatingActionButton(
                    onClick = { showNewGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Group Chat")
                }
            }
        }
    ) { paddingValues ->
        
        // The provided snippet for showRenameDialog and Column(g && groups.isEmpty())
        // appears to be intended for ChatRoomScreen or contains syntax errors.
        // To maintain syntactical correctness and adhere to the context of GroupListScreen,
        // I will only apply the paddingValues change and keep the existing logic for GroupListScreen.
        // If the rename dialog was intended for GroupListScreen, it would require additional context
        // like a selected group and onUpdateGroupName callback for this screen.

        if (isLoading && groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(groups) { group ->
                    GroupItem(group, onGroupClick)
                    HorizontalDivider()
                }
                if (groups.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No class groups found.")
                        }
                    }
                }
            }
        }
        
        if (showContactsDialog) {
            AlertDialog(
                onDismissRequest = { showContactsDialog = false },
                title = { Text("New Message") },
                text = {
                    LazyColumn {
                        items(contacts) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showContactsDialog = false
                                        onContactClick(contact)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = contact.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(text = contact.role.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                        if (contacts.isEmpty()) {
                            item { Text("No contacts available.", modifier = Modifier.padding(16.dp)) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContactsDialog = false }) { Text("Close") }
                }
            )
        }
        
        if (showNewGroupDialog) {
            var dept by remember { mutableStateOf("") }
            var year by remember { mutableStateOf("") }
            var section by remember { mutableStateOf("") }
            var subject by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showNewGroupDialog = false },
                title = { Text("Start Class Group Chat") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter the exact class details to join or start a group chat.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = dept, 
                            onValueChange = { dept = it }, 
                            label = { Text("Department (e.g. CSE)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = year, 
                            onValueChange = { year = it }, 
                            label = { Text("Year (e.g. 3)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = section, 
                            onValueChange = { section = it }, 
                            label = { Text("Section (e.g. A)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = subject, 
                            onValueChange = { subject = it }, 
                            label = { Text("Subject (e.g. Mathematics)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (dept.isNotBlank() && year.isNotBlank() && section.isNotBlank() && subject.isNotBlank()) {
                            val subjectFormatted = subject.trim().replace(" ", "_")
                            val groupId = "${dept.trim().uppercase()}-${year.trim()}-${section.trim().uppercase()}-T${currentUserId}-${subjectFormatted.uppercase()}"
                            val newGroup = ChatGroup(
                                group_id = groupId,
                                name = "${dept.trim().uppercase()} ${year.trim()}-${section.trim().uppercase()} (${subject.trim()})",
                                department = dept.trim().uppercase(),
                                year = year.toIntOrNull() ?: 1,
                                section = section.trim().uppercase(),
                                last_message = null,
                                unread_count = 0
                            )
                            showNewGroupDialog = false
                            onGroupClick(newGroup)
                        }
                    }) {
                        Text("Start Chat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewGroupDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun GroupItem(group: ChatGroup, onClick: (ChatGroup) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(group) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!group.profile_image.isNullOrEmpty()) {
                val avatarUrl = if (group.profile_image.startsWith("http")) group.profile_image else "${Constants.BASE_URL.removeSuffix("/")}${group.profile_image}"
                coil.compose.AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = group.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = group.last_message?.content ?: "No messages yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1
            )
        }
        
        if (group.unread_count > 0) {
            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                Text(group.unread_count.toString(), modifier = Modifier.padding(4.dp))
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    group: ChatGroup,
    messages: List<ChatMessageDto>,
    currentUserId: Int,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendFile: (Uri, String) -> Unit,
    onBack: () -> Unit,
    isEditing: Boolean,
    onCancelEdit: () -> Unit,
    onBeginEdit: (ChatMessageDto) -> Unit,
    onDelete: (Int) -> Unit,
    onDeleteGroup: () -> Unit,
    onUpdateGroupName: (String) -> Unit,
    onUploadGroupImage: (java.io.File) -> Unit,
    onDuelRequest: (Int, String) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            onSendFile(uri, mimeType) 
        }
    }
    
    val profileImageLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = getFileFromUri(context, uri)
            if (file != null) {
                onUploadGroupImage(file)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!group.profile_image.isNullOrEmpty()) {
                                val avatarUrl = if (group.profile_image.startsWith("http")) group.profile_image else "${Constants.BASE_URL.removeSuffix("/")}${group.profile_image}"
                                coil.compose.AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = group.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(group.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            val subtitle = if (group.department != null) "${group.department} ${group.year ?: ""}-${group.section ?: ""}" else ""
                            if (subtitle.isNotEmpty()) {
                                Text(
                                    subtitle, 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF075E54) // WhatsApp Teal Green
                ),
                actions = {
                    if (!group.group_id.startsWith("DM_")) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Group Profile") },
                                onClick = {
                                    showMenu = false
                                    profileImageLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Group") },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Group", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDeleteGroup()
                                }
                            )
                        }
                    } else {
                         // DM Specific Options (DUEL)
                         var showDMMenu by remember { mutableStateOf(false) }
                         IconButton(onClick = { showDMMenu = true }) {
                             Icon(Icons.Default.SportsEsports, contentDescription = "Challenge", tint = Color.White)
                         }
                         DropdownMenu(
                            expanded = showDMMenu,
                            onDismissRequest = { showDMMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Challenge to Duel ⚔️") },
                                onClick = {
                                    showDMMenu = false
                                    onDuelRequest(50, "MIXED") // Default hardcode for now or show dialog
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageText, 
                onValueChange = onMessageChange, 
                onSend = onSend,
                onAttach = { launcher.launch("*/*") },
                isEditing = isEditing,
                onCancelEdit = onCancelEdit
            )
        }
    ) { padding ->
        if (showRenameDialog) {
            var newName by remember { mutableStateOf(group.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Group") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Group Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotBlank()) {
                            onUpdateGroupName(newName.trim())
                            showRenameDialog = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Messages
        val listState = rememberLazyListState()
        
        // Auto scroll to bottom when new message arrives
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(0) 
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE5DDD5)) // WhatsApp Chat Background
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                reverseLayout = true, 
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 8.dp, top = 8.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(
                        msg = msg, 
                        isMe = msg.sender_id == currentUserId,
                        onDelete = { onDelete(msg.id) },
                        onEdit = { onBeginEdit(msg) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    msg: ChatMessageDto, 
    isMe: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val baseUrl = Constants.BASE_URL.removeSuffix("/")
    var showMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe && !msg.group_id.startsWith("DM_")) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!msg.sender_profile_image.isNullOrEmpty()) {
                    val avatarUrl = if (msg.sender_profile_image.startsWith("http")) msg.sender_profile_image else "$baseUrl${msg.sender_profile_image}"
                    coil.compose.AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = msg.sender_name?.take(1)?.uppercase() ?: "U",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // Sender Name for group chats (others)
            if (!isMe && !msg.group_id.startsWith("DM_") && msg.sender_name != null) {
                Text(
                    text = msg.sender_name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF128C7E), // Dark Teal
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
            
            Box {
                Surface(
                    shape = if (isMe) 
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 0.dp)
                    else 
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 8.dp),
                    color = if (isMe) Color(0xFFDCF8C6) else Color.White, // WhatsApp Green or White
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (isMe && !msg.is_deleted) { // Only allow edit/delete own messages
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                            }
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Attachment Handling
                    if (msg.attachment_url != null) {
                        val fullUrl = if (msg.attachment_url.startsWith("http")) msg.attachment_url else "$baseUrl${msg.attachment_url}"
                        
                        when (msg.msg_type) {
                            "IMAGE" -> {
                                coil.compose.AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clickable { uriHandler.openUri(fullUrl) }
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            "VIDEO" -> {
                                FileAttachmentItem(
                                    icon = Icons.Default.PlayCircleOutline,
                                    filename = msg.filename ?: "Video",
                                    color = Color.Red,
                                    onClick = { uriHandler.openUri(fullUrl) }
                                )
                            }
                            "PDF" -> {
                                 FileAttachmentItem(
                                    icon = Icons.Default.Description,
                                    filename = msg.filename ?: "PDF Document",
                                    color = Color.Red,
                                    onClick = { uriHandler.openUri(fullUrl) }
                                )
                            }
                            else -> { // FILE or other
                                FileAttachmentItem(
                                    icon = Icons.Default.InsertDriveFile,
                                    filename = msg.filename ?: "File",
                                    color = Color.Blue,
                                    onClick = { uriHandler.openUri(fullUrl) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (msg.msg_type == "DUEL" && !msg.is_deleted && !msg.content.isNullOrBlank()) {
                            DuelMessageCard(msgContent = msg.content, isMe = isMe, modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp))
                        } else {
                            Text(
                                text = if (msg.content.isNullOrBlank()) "(No content)" else msg.content,
                                color = if (msg.is_deleted) Color.Gray else Color.Black,
                                style = if (msg.is_deleted) MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) else MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                            )
                        }
                        
                        Text(
                            text = formatTime(msg.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Context Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Edit only if TEXT and not deleted
                if (msg.attachment_url == null && !msg.is_deleted) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
                
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
            }
        }
    }
}

@Composable
fun DuelMessageCard(msgContent: String, isMe: Boolean, modifier: Modifier = Modifier) {
    var wager = 0
    var category = "MIXED"
    var status = "PENDING"
    try {
        val json = org.json.JSONObject(msgContent)
        wager = json.optInt("wager", 0)
        category = json.optString("category", "MIXED")
        status = json.optString("status", "PENDING")
    } catch (e: Exception) {
        // Handle gracefully
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFFE65100))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quiz Duel \u2694\uFE0F", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE65100))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Wager: $wager XP", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Category: $category", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        
        Spacer(modifier = Modifier.height(8.dp))
        if (status == "PENDING") {
            if (isMe) {
                Text("Waiting for opponent...", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            } else {
                Button(
                    onClick = { /* TODO: Launch QuizDuelScreen */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Accept Match")
                }
            }
        } else {
            Text("Match $status", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
    }
}

@Composable
fun FileAttachmentItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filename: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0x11000000), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = filename,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    isEditing: Boolean = false,
    onCancelEdit: () -> Unit = {}
) {
    Column {
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0F2F1))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF008069))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Editing Message", style = MaterialTheme.typography.labelMedium, color = Color(0xFF008069))
                    Text(text, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(onClick = onCancelEdit) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F0F0)) // Light Gray Bar
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment Button
            if (!isEditing) {
                IconButton(
                    onClick = onAttach,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color.Gray)
                }
            } else {
                 Spacer(modifier = Modifier.width(8.dp))
            }

            // Text Input
            TextField(
                value = text,
                onValueChange = onValueChange,
                placeholder = { Text(if (isEditing) "Edit message..." else "Message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )
            
            // Send Button
            FloatingActionButton(
                onClick = onSend,
                containerColor = Color(0xFF008069), // WhatsApp Green
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Send, 
                    contentDescription = if (isEditing) "Update" else "Send", 
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

fun formatTime(isoString: String): String {
    return try {
        // Backend datetime.utcnow().isoformat() returns "YYYY-MM-DDTHH:MM:SS.ffffff" without 'Z'
        val normalizedString = if (isoString.endsWith("Z")) isoString else "${isoString}Z"
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.ENGLISH)
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC") 
        
        // Handle variations in fractional seconds lengths by using a resilient parser
        val date = if(isoString.contains(".")) {
             // Let's use a simpler fallback to just truncate fractional seconds and Z
             val baseStr = isoString.substringBefore(".")
             val parserTruncated = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ENGLISH)
             parserTruncated.timeZone = java.util.TimeZone.getTimeZone("UTC")
             parserTruncated.parse(baseStr)
        } else {
             val baseStr = if (isoString.endsWith("Z")) isoString.dropLast(1) else isoString
             val parserTruncated = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ENGLISH)
             parserTruncated.timeZone = java.util.TimeZone.getTimeZone("UTC")
             parserTruncated.parse(baseStr)
        }

        val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH)
        formatter.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") // Strongly lock to Indian Time
        
        if (date != null) formatter.format(date) else ""
    } catch (e: Exception) {
        if(isoString.length >= 16) isoString.substring(11, 16) else isoString
    }
}
