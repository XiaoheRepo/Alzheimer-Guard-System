package com.xiaohelab.guard.android.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HC-07: JWT tokens are stored in [EncryptedSharedPreferences] only.
 * No other persistence (Room / DataStore) is allowed to hold raw tokens.
 *
 * Tokens are kept as `String` (HC-ID-String semantics still apply to user_id).
 */
@Singleton
class AuthTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _session = MutableStateFlow(read())
    val session: StateFlow<AuthSession?> = _session

    fun accessToken(): String? = _session.value?.accessToken
    fun refreshToken(): String? = _session.value?.refreshToken
    fun userId(): String? = _session.value?.userId
    fun role(): String? = _session.value?.role
    fun hasSession(): Boolean = _session.value != null

    fun save(session: AuthSession) {
        prefs.edit().apply {
            putString(K_ACCESS, session.accessToken)
            putString(K_REFRESH, session.refreshToken)
            putString(K_USER_ID, session.userId)
            putString(K_ROLE, session.role)
            putLong(K_EXP, session.expiresAtEpochSeconds)
        }.apply()
        _session.value = session
    }

    fun updateTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        val current = _session.value ?: return
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = refreshToken ?: current.refreshToken,
            expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + expiresInSeconds,
        )
        save(updated)
    }

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun read(): AuthSession? {
        val access = prefs.getString(K_ACCESS, null) ?: return null
        val refresh = prefs.getString(K_REFRESH, null)
        val userId = prefs.getString(K_USER_ID, null) ?: return null
        val role = prefs.getString(K_ROLE, null) ?: return null
        val exp = prefs.getLong(K_EXP, 0)
        return AuthSession(access, refresh, userId, role, exp)
    }

    companion object {
        private const val FILE = "mh_auth_secure_prefs"
        private const val K_ACCESS = "access_token"
        private const val K_REFRESH = "refresh_token"
        private const val K_USER_ID = "user_id"
        private const val K_ROLE = "role"
        private const val K_EXP = "expires_at"
    }
}

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String,
    val role: String,
    val expiresAtEpochSeconds: Long,
) {
    companion object {
        /** Handbook §16 / HC-Auth 角色: 家属端仅允许 FAMILY。 */
        const val ROLE_FAMILY = "FAMILY"
    }
}
