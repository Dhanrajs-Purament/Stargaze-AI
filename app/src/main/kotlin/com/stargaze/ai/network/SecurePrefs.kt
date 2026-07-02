package com.stargaze.ai.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted key-value storage backed by the Android Keystore (AES-256-GCM).
 *
 * Used for any sensitive client-side value such as a session token issued by the AI proxy. The LLM
 * API key itself is never stored on-device — it lives only on the proxy. This wrapper exists so
 * that if/when the proxy issues a short-lived client token, it is stored encrypted at rest rather
 * than in plaintext SharedPreferences.
 */
class SecurePrefs(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)

    fun setSessionToken(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) remove(KEY_SESSION_TOKEN) else putString(KEY_SESSION_TOKEN, token)
        }.apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val FILE_NAME = "stargaze_secure_prefs"
        const val KEY_SESSION_TOKEN = "session_token"
    }
}
