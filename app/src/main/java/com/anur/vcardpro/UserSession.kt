package com.anur.vcardpro

import android.content.Context

object UserSession {
    private const val PREFS_NAME = "VCardApp_Session"
    private const val KEY_USER_ID = "userId"
    private const val KEY_USER_NAME = "userName"
    private const val KEY_USER_EMAIL = "userEmail"

    private var _userId: Int = -1
    private var _userName: String = ""
    private var _userEmail: String = ""

    // Public getters
    var userId: Int
        get() = _userId
        set(value) {
            _userId = value
        }

    var userName: String
        get() = _userName
        set(value) {
            _userName = value
        }

    var userEmail: String
        get() = _userEmail
        set(value) {
            _userEmail = value
        }

    // Save session to SharedPreferences
    fun saveSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_USER_ID, _userId)
            .putString(KEY_USER_NAME, _userName)
            .putString(KEY_USER_EMAIL, _userEmail)
            .apply()
    }

    // Load session from SharedPreferences
    fun loadSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _userId = prefs.getInt(KEY_USER_ID, -1)
        _userName = prefs.getString(KEY_USER_NAME, "") ?: ""
        _userEmail = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    // Clear session
    fun clearSession(context: Context) {
        _userId = -1
        _userName = ""
        _userEmail = ""

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return _userId != -1
    }
}