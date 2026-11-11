package com.auraface.auraface_app.data.local.preferences

import android.content.Context

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences("auth_prefs_standard", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun saveRole(role: String) {
        prefs.edit().putString("role", role).apply()
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun getRole(): String? {
        return prefs.getString("role", null)
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }

    fun saveFlags(isHod: Boolean, isClassTeacher: Boolean) {
        prefs.edit()
            .putBoolean("is_hod", isHod)
            .putBoolean("is_class_teacher", isClassTeacher)
            .apply()
    }

    fun isHod(): Boolean {
        return prefs.getBoolean("is_hod", false)
    }

    fun isClassTeacher(): Boolean {
        return prefs.getBoolean("is_class_teacher", false)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString("fcm_token", token).apply()
    }

    fun getFcmToken(): String? {
        return prefs.getString("fcm_token", null)
    }
}

