package com.auraface.auraface_app.data.remote

import android.util.Log
import com.auraface.auraface_app.data.remote.dto.ChatMessageDto
import com.google.gson.Gson
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatWebSocketManager @Inject constructor(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    
    private val _messageFlow = MutableSharedFlow<ChatMessageDto>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()

    fun connect(url: String, token: String) {
        disconnect() // Ensure previous connection closed
        val request = Request.Builder()
            .url("$url?token=$token")
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatWebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, ChatMessageDto::class.java)
                    _messageFlow.tryEmit(msg)
                } catch (e: Exception) {
                    Log.e("ChatWebSocket", "Parsing error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatWebSocket", "Error: ${t.message}")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ChatWebSocket", "Closed: $reason")
            }
        })
    }

    fun sendMessage(content: String, group_id: String, type: String = "TEXT", attachmentUrl: String? = null, replyToId: Int? = null) {
        val payload = mapOf(
            "content" to content,
            "msg_type" to type,
            "attachment_url" to attachmentUrl,
            "reply_to_id" to replyToId,
            "group_id" to group_id
        )
        val json = gson.toJson(payload)
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User requested")
        webSocket = null
    }
}
