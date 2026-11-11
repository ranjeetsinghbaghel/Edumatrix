package com.auraface.auraface_app.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: String,
    val imageBase64: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
