package com.auraface.auraface_app.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.auraface.auraface_app.R

object NotificationHelper {
    const val CHANNEL_ID = "auraface_alerts_channel_v2"
    private const val CHANNEL_NAME = "EduMatrix Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Channel for EduMatrix alerts and notices"
                enableVibration(true)
                try {
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                } catch (e: Exception) {
                    // Ignore if field not available
                }
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showLowAttendanceAlert(context: Context, subjectName: String, percentage: Float) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon later
            .setContentTitle("Low Attendance Warning!")
            .setContentText("Your attendance in $subjectName is $percentage%. Please attend classes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
