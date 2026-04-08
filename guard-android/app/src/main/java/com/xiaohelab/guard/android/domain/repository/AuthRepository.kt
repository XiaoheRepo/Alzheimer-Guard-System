package com.xiaohelab.guard.android.domain.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.LoginResult
import com.xiaohelab.guard.android.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): ApiResult<LoginResult>
    suspend fun register(username: String, password: String, phone: String, role: String): ApiResult<User>
    suspend fun logout(): ApiResult<Unit>
    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit>
    suspend fun getMe(): ApiResult<User>
}
