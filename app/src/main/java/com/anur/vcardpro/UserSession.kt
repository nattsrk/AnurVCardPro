package com.anur.vcardpro

import android.content.Context

object UserSession {
    var userId: Int = -1
    var userName: String = ""
    var userEmail: String = ""

    fun isLoggedIn(): Boolean = userId != -1

    fun saveSession(context: Context) {
        val prefs = context.getSharedPreferences("VCardApp", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("userId", userId)
            putString("userName", userName)
            putString("userEmail", userEmail)
            apply()
        }
    }

    fun loadSession(context: Context) {
        val prefs = context.getSharedPreferences("VCardApp", Context.MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)
        userName = prefs.getString("userName", "") ?: ""
        userEmail = prefs.getString("userEmail", "") ?: ""
    }

    fun clearSession(context: Context) {
        userId = -1
        userName = ""
        userEmail = ""
        val prefs = context.getSharedPreferences("VCardApp", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}