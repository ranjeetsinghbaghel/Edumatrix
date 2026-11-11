package com.auraface.auraface_app.data.repository

import android.content.Context
import androidx.work.*
import com.auraface.auraface_app.data.local.database.AttendanceDao
import com.auraface.auraface_app.data.local.database.AttendanceEntity
import com.auraface.auraface_app.data.network.api.AttendanceApi
import com.auraface.auraface_app.data.network.model.AttendanceRequest
import com.auraface.auraface_app.data.network.model.AttendanceResponse
import com.auraface.auraface_app.worker.SyncAttendanceWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AttendanceRepository @Inject constructor(
    private val api: AttendanceApi,
    private val attendanceDao: AttendanceDao,
    @ApplicationContext private val context: Context
) {
    suspend fun markAttendance(
        subjectId: String,
        period: String,
        imageBase64: String,
        startTime: String? = null,
        endTime: String? = null
    ): AttendanceResponse {
        // Save to local DB first (keeping simple for now)
        val entity = AttendanceEntity(
            subjectId = subjectId,
            imageBase64 = imageBase64
        )
        attendanceDao.insertAttendance(entity)

        // Trigger background sync
        scheduleSync()

        // Attempt direct API call
        return try {
            val apiRes = api.markAttendance(
                AttendanceRequest(
                    image_base64 = imageBase64,
                    subject_id = subjectId,
                    period = period,
                    start_time = startTime,
                    end_time = endTime
                )
            )
            
            if (apiRes.success && apiRes.data != null) {
                // If successful, mark as synced
                attendanceDao.updateAttendance(entity.copy(isSynced = true))
                apiRes.data
            } else {
                 AttendanceResponse(
                    success = false,
                    message = apiRes.error?.message ?: apiRes.message ?: "Unknown error"
                 )
            }
        } catch (e: Exception) {
            // Return a mock/partial response if offline
            AttendanceResponse(
                success = true,
                message = "Attendance saved offline. It will sync once internet is available."
            )
        }
    }

    suspend fun markAttendanceManual(
        subjectId: String,
        period: String,
        rollNo: String,
        startTime: String? = null,
        endTime: String? = null
    ): AttendanceResponse {
        val res = api.markAttendanceManual(
            com.auraface.auraface_app.data.network.model.ManualAttendanceRequest(
                roll_no = rollNo,
                subject_id = subjectId,
                period = period,
                start_time = startTime,
                end_time = endTime
            )
        )
        if (res.success && res.data != null) return res.data
        throw Exception(res.error?.message ?: res.message)
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncAttendanceWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "attendance_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    suspend fun getAttendance() = api.getAttendance()

    suspend fun getStudentsByClass(year: Int, section: String) = 
        api.getStudentsByClass(year, section)

    suspend fun markBulkAttendance(studentIds: List<Int>, subjectId: String, period: String) =
        api.markBulkAttendance(
            com.auraface.auraface_app.data.network.model.BulkAttendanceMark(
                student_ids = studentIds,
                subject_id = subjectId,
                period = period
            )
        )
}


