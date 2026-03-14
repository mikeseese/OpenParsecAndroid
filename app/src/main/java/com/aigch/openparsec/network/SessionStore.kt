package com.aigch.openparsec.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aigch.openparsec.OpenParsecApp
import org.json.JSONObject

/**
 * Secure session storage using SharedPreferences.
 * Ported from iOS Keychain storage in ContentView/LoginView.
 */
object SessionStore {
    private const val TAG = "SessionStore"
    private const val PREFS_NAME = "openparsec_session"
    private const val KEY_SESSION_DATA = "session_data"

    private val prefs: SharedPreferences by lazy {
        OpenParsecApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(clientInfo: ClientInfo) {
        val json = JSONObject().apply {
            put("instance_id", clientInfo.instance_id)
            put("user_id", clientInfo.user_id)
            put("session_id", clientInfo.session_id)
            put("host_peer_id", clientInfo.host_peer_id)
        }
        prefs.edit().putString(KEY_SESSION_DATA, json.toString()).apply()
        Log.d(TAG, "Session data saved")
    }

    fun load(): ClientInfo? {
        val jsonStr = prefs.getString(KEY_SESSION_DATA, null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            ClientInfo(
                instance_id = json.optString("instance_id", ""),
                user_id = json.optInt("user_id", 0),
                session_id = json.optString("session_id", ""),
                host_peer_id = json.optString("host_peer_id", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading session: ${e.message}")
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION_DATA).apply()
        Log.d(TAG, "Session data cleared")
    }
}
