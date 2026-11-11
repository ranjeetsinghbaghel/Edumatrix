package com.auraface.auraface_app.core

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkErrorMapper {
    fun toUserMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "Server not found. Check BASE_URL or internet connection."
            is ConnectException -> "Cannot reach server. Connect phone and server to same Wi-Fi, and keep port 8000 open."
            is SocketTimeoutException -> "Server timed out. Check backend status and network speed."
            else -> {
                val raw = error.localizedMessage.orEmpty()
                if (raw.contains("Failed to connect", ignoreCase = true)) {
                    "Cannot reach server. Connect phone and server to same Wi-Fi, and keep port 8000 open."
                } else {
                    "Network request failed. Please try again."
                }
            }
        }
    }
}
