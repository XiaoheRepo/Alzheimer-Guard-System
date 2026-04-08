package com.xiaohelab.guard.android.data.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.TokenManager
import com.xiaohelab.guard.android.core.network.NetworkModule.safeApiCall
import com.xiaohelab.guard.android.data.mapper.toDomain
import com.xiaohelab.guard.android.data.remote.api.AuthApiService
import com.xiaohelab.guard.android.data.remote.dto.ChangePasswordRequestDto
import com.xiaohelab.guard.android.data.remote.dto.LoginRequestDto
import com.xiaohelab.guard.android.data.remote.dto.RegisterRequestDto
import com.xiaohelab.guard.android.domain.model.LoginResult
import com.xiaohelab.guard.android.domain.model.User
import com.xiaohelab.guard.android.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): ApiResult<LoginResult> {
        val result = safeApiCall { api.login(LoginRequestDto(username, password)) }
        if (result is ApiResult.Success) {
            tokenManager.saveSession(
                token = result.data.accessToken,
                userId = result.data.userId,
                role = result.data.role
            )
        }
        return result
    }

    override suspend fun register(
        username: String, password: String, phone: String, role: String
    ): ApiResult<User> = safeApiCall { api.register(RegisterRequestDto(username, password, phone, role)) }
        .map { it.toDomain() }

    override suspend fun logout(): ApiResult<Unit> {
        val result = safeApiCall { api.logout() }
        tokenManager.clearSession()
        return result
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit> =
        safeApiCall { api.changePassword(ChangePasswordRequestDto(oldPassword, newPassword)) }

    override suspend fun getMe(): ApiResult<User> =
        safeApiCall { api.getMe() }.map { it.toDomain() }
}
