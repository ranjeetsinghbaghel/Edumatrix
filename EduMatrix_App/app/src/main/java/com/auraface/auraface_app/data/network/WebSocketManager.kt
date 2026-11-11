package com.auraface.auraface_app.data.network

import android.util.Log
import com.auraface.auraface_app.core.Constants
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.auraface.auraface_app.data.network.model.ChatMessageDto

@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    
    // Flow to emit received messages to ViewModel
    private val _messageFlow = MutableSharedFlow<ChatMessageDto>(extraBufferCapacity = 64)
    val messageFlow = _messageFlow.asSharedFlow()

    fun connect(token: String) {
        if (webSocket != null) return
        val wsBaseUrl = Constants.BASE_URL
            .removeSuffix("/")
            .replaceFirst("http://", "ws://")
            .replaceFirst("https://", "wss://")
        val request = Request.Builder()
            .url("$wsBaseUrl/chat/ws?token=$token")
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected to global websocket")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Message received: $text")
                try {
                    val json = JSONObject(text)
                    val msg = ChatMessageDto(
                        id = json.optInt("id"),
                        sender_id = json.optInt("sender_id"),
                        sender_name = json.optString("sender_name", "").takeIf { it.isNotEmpty() && it != "null" },
                        sender_profile_image = json.optString("sender_profile_image", "").takeIf { it.isNotEmpty() && it != "null" },
                        group_id = json.optString("group_id"),
                        content = json.optString("content"),
                        msg_type = json.optString("msg_type", "TEXT"),
                        attachment_url = json.optString("attachment_url", "").takeIf { it.isNotEmpty() && it != "null" },
                        filename = json.optString("filename", "").takeIf { it.isNotEmpty() && it != "null" },
                        timestamp = json.optString("timestamp"), // ISO
                        status = "DELIVERED"
                    )
                    _messageFlow.tryEmit(msg)
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message: ${e.message}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $code / $reason")
                this@WebSocketManager.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
                this@WebSocketManager.webSocket = null
            }
        })
    }
    
    fun subscribeToGroups(groupIds: List<String>) {
        val json = JSONObject().apply {
            put("action", "subscribe")
            put("groups", org.json.JSONArray(groupIds))
        }
        webSocket?.send(json.toString())
    }
    
    fun sendMessage(groupId: String, content: String?, msgType: String = "TEXT", attachmentUrl: String? = null, filename: String? = null) {
        val json = JSONObject().apply {
            put("action", "send")
            put("group_id", groupId)
            put("content", content)
            put("msg_type", msgType)
            if (attachmentUrl != null) put("attachment_url", attachmentUrl)
            if (filename != null) put("filename", filename)
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User left chat")
        webSocket = null
    }
}
