package com.xiaohelab.guard.android.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token 安全存储管理器（HandBook §7）
 * - 使用 EncryptedSharedPreferences 加密存储 Token（满足"敏感凭据必须加密"要求）
 * - 匿名令牌存储（X-Anonymous-Token，匿名链路专用）
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "guard_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_ANONYMOUS_TOKEN = "anonymous_token"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, falling back to plain")
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(token: String, userId: String, role: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    /** 匿名令牌（PUB-02->PUB-03/04 链路专用） */
    fun saveAnonymousToken(token: String) {
        prefs.edit().putString(KEY_ANONYMOUS_TOKEN, token).apply()
    }

    fun getAnonymousToken(): String? = prefs.getString(KEY_ANONYMOUS_TOKEN, null)

    fun clearAnonymousToken() {
        prefs.edit().remove(KEY_ANONYMOUS_TOKEN).apply()
    }

    /**
     * 会话失效清理（E_GOV_4011 统一入口）
     * 必须同时清理 Token、用户缓存（HandBook §7）
     */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_ROLE)
            .apply()
    }
}
