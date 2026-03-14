package com.nitrous.docanalyzer.auth.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nitrous.docanalyzer.mapper.toTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _authState = MutableStateFlow(isLoggedIn())
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    fun saveSession(accessToken: String, refreshToken: String, expiresAt: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_EXPIRES_AT, expiresAt)
        }.apply()
        _authState.value = true
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getExpiresAt(): String? = prefs.getString(KEY_EXPIRES_AT, null)

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_AT)
        }.apply()
        _authState.value = false
    }

    fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        val expiresAtIso = getExpiresAt()
        
        if (token == null || expiresAtIso == null) return false
        
        return try {
            val expiryTime = expiresAtIso.toTimestamp()
            // Check if expiration is at least 30 seconds in the future
            expiryTime > (System.currentTimeMillis() + 30000L)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
