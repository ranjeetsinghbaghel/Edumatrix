package com.auraface.auraface_app.core.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.app.NotificationManager
import com.auraface.auraface_app.data.repository.SmartFeaturesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smartFeaturesRepository: SmartFeaturesRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val noticeId = intent.getIntExtra("notice_id", -1)
        val notificationId = intent.getIntExtra("notification_id", -1)

        if (action == "MARK_AS_READ" && noticeId != -1) {
            Log.d("NotificationReceiver", "Marking notice $noticeId as read")
            
            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // API Call
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    smartFeaturesRepository.markNoticeRead(noticeId)
                    Log.d("NotificationReceiver", "Notice $noticeId marked as read successfully")
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Failed to mark notice read", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
