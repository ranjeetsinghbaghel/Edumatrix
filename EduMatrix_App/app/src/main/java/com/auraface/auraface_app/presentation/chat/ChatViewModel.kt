package com.auraface.auraface_app.presentation.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.network.WebSocketManager
import com.auraface.auraface_app.data.network.model.ChatGroup
import com.auraface.auraface_app.data.network.model.ChatMessageDto
import com.auraface.auraface_app.data.repository.ChatRepository
import com.auraface.auraface_app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import com.auraface.auraface_app.data.local.preferences.TokenManager
import com.auraface.auraface_app.data.repository.QuizRepository
import com.auraface.auraface_app.data.network.model.DuelWagerCreate

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ChatRepository,
    private val authRepository: AuthRepository,
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager,
    private val quizRepository: QuizRepository
) : ViewModel() {

    var groups = mutableStateListOf<ChatGroup>()
    var contacts = mutableStateListOf<com.auraface.auraface_app.data.network.model.ChatContact>()
    var messages = mutableStateListOf<ChatMessageDto>()
    var currentGroup: ChatGroup? by mutableStateOf(null)
    
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    
    var newMessageText by mutableStateOf("")
    var currentUserId by mutableStateOf(0)
    var editingMessageId by mutableStateOf<Int?>(null)

    init {
        loadCurrentUser()
        loadGroups()
        loadContacts()
        
        // Listen for new messages from WS
        viewModelScope.launch {
            webSocketManager.messageFlow.collectLatest { msg ->
                if (currentGroup?.group_id == msg.group_id) {
                    val existingIndex = messages.indexOfFirst { it.id == msg.id }
                    if (existingIndex != -1) {
                        // Update existing message (Edit/Delete)
                        messages[existingIndex] = msg
                        // If we were editing this message, stop editing
                        if (editingMessageId == msg.id && (msg.is_deleted || msg.content != newMessageText)) {
                             // Optional: Cancel edit if it was deleted remotely
                        }
                    } else {
                        // New message
                        messages.add(0, msg)
                    }
                } else {
                     // Update last message in group list
                     val index = groups.indexOfFirst { it.group_id == msg.group_id }
                     if (index != -1) {
                         val group = groups[index]
                         val isUnread = if (msg.sender_id != currentUserId) 1 else 0
                         groups[index] = group.copy(last_message = msg, unread_count = group.unread_count + isUnread)
                     }
                     
                     // Show notification for unseen chat if not sent by us
                     if (msg.sender_id != currentUserId) {
                         showNotification(msg, groups.getOrNull(index)?.name ?: "Group Chat")
                     }
                }
            }
        }
    }

    private fun showNotification(msg: ChatMessageDto, groupName: String) {
        val channelId = "chat_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("New message in $groupName")
            .setContentText("${msg.sender_name ?: "Someone"}: ${msg.content ?: msg.msg_type}")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(msg.id, notification)
    }

    fun startEditing(msg: ChatMessageDto) {
        editingMessageId = msg.id
        newMessageText = msg.content ?: ""
    }

    fun cancelEditing() {
        editingMessageId = null
        newMessageText = ""
    }
    
    // ... existing ...

    fun deleteMessage(msgId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(msgId)
            } catch (e: Exception) {
                error = "Failed to delete: ${e.message}"
            }
        }
    }

    fun editMessage(msgId: Int, newContent: String) {
        viewModelScope.launch {
            try {
                repository.editMessage(msgId, newContent)
            } catch (e: Exception) {
                error = "Failed to edit: ${e.message}"
            }
        }
    }

    // ... existing ...
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                // Returns UserProfile directly, will throw if error
                val profile = authRepository.getProfile()
                currentUserId = profile.id ?: 0
            } catch (e: Exception) {
                // handle error or default to 0
                currentUserId = 0
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = repository.getMyGroups()
                if (response.isSuccessful) {
                    groups.clear()
                    groups.addAll(response.body() ?: emptyList())
                    
                    // Connect and Subscribe to all groups globally
                    val token = tokenManager.getToken()
                    if (token != null) {
                        webSocketManager.connect(token)
                        webSocketManager.subscribeToGroups(groups.map { it.group_id })
                    }
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            try {
                val response = repository.getContacts()
                if (response.isSuccessful) {
                    contacts.clear()
                    contacts.addAll(response.body() ?: emptyList())
                }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun enterChat(group: ChatGroup) {
        currentGroup = group
        // Reset unread count when entering chat
        val index = groups.indexOfFirst { it.group_id == group.group_id }
        if (index != -1) {
            groups[index] = groups[index].copy(unread_count = 0)
        }
        
        viewModelScope.launch {
            isLoading = true
            try {
                // Fetch History
                val response = repository.getChatHistory(group.group_id)
                if (response.isSuccessful) {
                    messages.clear()
                    messages.addAll(response.body() ?: emptyList())
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }
    
    fun startDM(contact: com.auraface.auraface_app.data.network.model.ChatContact) {
        val myId = currentUserId
        val theirId = contact.id
        val groupId = "DM_${minOf(myId, theirId)}_${maxOf(myId, theirId)}"
        
        val existingGroup = groups.find { it.group_id == groupId }
        if (existingGroup != null) {
            enterChat(existingGroup)
        } else {
            val newGroup = ChatGroup(
                group_id = groupId,
                name = contact.name,
                department = null, year = null, section = null,
                last_message = null, unread_count = 0
            )
            groups.add(0, newGroup)
            // also subscribe to this new DM group globally
            webSocketManager.subscribeToGroups(listOf(groupId))
            enterChat(newGroup)
        }
    }

    fun leaveChat() {
        currentGroup = null
        messages.clear()
        // we keep the global websocket connected
    }
    
    fun sendMessage() {
        if (newMessageText.isBlank()) return
        
        if (editingMessageId != null) {
            editMessage(editingMessageId!!, newMessageText)
            editingMessageId = null
            newMessageText = ""
        } else {
            val currentGroupId = currentGroup?.group_id ?: return
            webSocketManager.sendMessage(currentGroupId, newMessageText)
            newMessageText = ""
        }
    }
    
    fun sendFile(file: java.io.File, mimeType: String) {
        val currentGroupId = currentGroup?.group_id ?: return
        viewModelScope.launch {
            try {
                // Determine message type
                val msgType = when {
                    mimeType.startsWith("image") -> "IMAGE"
                    mimeType.startsWith("video") -> "VIDEO"
                    mimeType.startsWith("application/pdf") -> "PDF"
                    else -> "FILE"
                }

                // Upload
                val response = repository.uploadFile(file)
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyMap()
                    val url = body["url"]
                    if (url != null) {
                        // Send via WS
                        webSocketManager.sendMessage(
                            groupId = currentGroupId,
                            content = file.name,
                            msgType = msgType,
                            attachmentUrl = url,
                            filename = file.name
                        )
                    }
                } else {
                    error = "Failed to upload file"
                }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    
    fun sendDuelXP(targetUserId: Int, amount: Int, category: String = "MIXED", onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = quizRepository.sendDuelInvite(DuelWagerCreate(targetUserId, amount, category))
                if (res.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Failed to send duel invite: XP insufficient or network error.")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown Error")
            }
        }
    }
    
    fun deleteGroup(groupId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteGroup(groupId)
                leaveChat()
                onDone()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun updateGroupName(groupId: String, name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateGroupMetadata(groupId, name)
                currentGroup = currentGroup?.copy(name = name)
                onDone()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun uploadGroupImage(groupId: String, file: java.io.File, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.uploadGroupImage(groupId, file)
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyMap()
                    val url = body["profile_image"]
                    if (url != null) {
                        currentGroup = currentGroup?.copy(profile_image = url)
                        onDone(url)
                    }
                }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
