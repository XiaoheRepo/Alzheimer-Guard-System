package com.xiaohelab.guard.android.feature.auth.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.feature.auth.data.UserProfileDto

/**
 * Auth-domain facade exposed to feature UI + other domains (HC-01 六域隔离).
 */
interface AuthRepository {
    suspend fun login(username: String, password: String): MhResult<UserProfileDto>
    suspend fun register(username: String, email: String, phone: String, password: String, nickname: String?): MhResult<Unit>
    suspend fun logout(): MhResult<Unit>
    suspend fun requestPasswordReset(email: String, locale: String?): MhResult<Unit>
    suspend fun confirmPasswordReset(token: String, newPassword: String): MhResult<Unit>
}
