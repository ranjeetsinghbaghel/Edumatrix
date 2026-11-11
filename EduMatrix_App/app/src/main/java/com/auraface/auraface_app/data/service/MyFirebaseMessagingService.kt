package com.auraface.auraface_app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.auraface.auraface_app.MainActivity
import com.auraface.auraface_app.R
import com.auraface.auraface_app.data.local.preferences.TokenManager
import com.auraface.auraface_app.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        tokenManager.saveFcmToken(token)
        
        // If we have an authentication token (meaning logged in), send to server immediately
        if (tokenManager.getToken() != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    authRepository.updateFcmToken(token)
                    Log.d(TAG, "Token sent to server successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send token to server", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Determine if emergency from data
        val isEmergency = remoteMessage.data["type"] == "EMERGENCY" || remoteMessage.data["priority"] == "HIGH"

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Handle Data-only Emergency Notification if no notification payload
            if (remoteMessage.notification == null) {
                val title = remoteMessage.data["title"]
                val body = remoteMessage.data["body"]
                sendNotification(title, body, isEmergency, data = remoteMessage.data)
                return
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body, isEmergency, data = remoteMessage.data)
        }
    }

    private fun sendNotification(title: String?, messageBody: String?, isEmergency: Boolean, data: Map<String, String>? = null) {
        // Broadcast for In-App handling
        if (isEmergency) {
            val broadcastIntent = Intent("com.auraface.auraface_app.EMERGENCY_ALERT")
            broadcastIntent.setPackage(packageName)
            broadcastIntent.putExtra("title", title)
            broadcastIntent.putExtra("body", messageBody)
            sendBroadcast(broadcastIntent)
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Add extra to identify source if needed
        intent.putExtra("is_emergency", isEmergency)
        intent.putExtra("title", title)
        intent.putExtra("body", messageBody)
        
        if (data?.get("type") == "CHAT") {
             val groupId = data["group_id"]
             if (groupId != null) {
                 intent.putExtra("open_chat_group_id", groupId)
             }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val noticeId = try {
            data?.get("notice_id")?.toInt() ?: -1
        } catch (e: Exception) { -1 }

        val notificationId = System.currentTimeMillis().toInt() // Unique ID

        // Create PendingIntent for Mark as Read
        val markReadIntent = Intent(this, com.auraface.auraface_app.core.receivers.NotificationActionReceiver::class.java).apply {
            action = "MARK_AS_READ"
            putExtra("notice_id", noticeId)
            putExtra("notification_id", notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = com.auraface.auraface_app.core.utils.NotificationHelper.CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) 
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            
        if (noticeId != -1) {
            notificationBuilder.addAction(R.drawable.ic_launcher_foreground, "Mark as Read", markReadPendingIntent)
        }
            
        if (isEmergency) {
            notificationBuilder.setFullScreenIntent(pendingIntent, true)
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists with high importance
        com.auraface.auraface_app.core.utils.NotificationHelper.createNotificationChannel(this)

        notificationManager.notify(if (isEmergency) 999 else notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
