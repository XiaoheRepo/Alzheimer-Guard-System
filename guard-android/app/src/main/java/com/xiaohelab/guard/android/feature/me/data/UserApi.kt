package com.xiaohelab.guard.android.feature.me.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import com.xiaohelab.guard.android.feature.auth.data.UserProfileDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * Me 域 API。
 * HC-08: 登录态接口由 AuthInterceptor 统一注入 Authorization，禁止携带 X-User-Id / X-User-Role。
 * HC-ID-String: user_id 为 String。
 */
interface UserApi {
    @GET("/api/v1/users/me")
    suspend fun me(): Response<ApiEnvelope<UserProfileDto>>

    /**
     * 修改当前登录用户密码。
     * 错误码: E_AUTH_4001（旧密码错误）/ E_AUTH_4003（复杂度不足）。
     * HC-06: 禁止 SMS 验证码链路；此处仅走 old+new 双密码校验。
     */
    @PUT("/api/v1/users/me/password")
    suspend fun changePassword(
        @Body body: ChangePasswordRequest,
    ): Response<ApiEnvelope<Unit>>
}

/**
 * 修改密码请求体（API V2.0 §users.me.password）。
 */
@Serializable
data class ChangePasswordRequest(
    @SerialName("old_password") val oldPassword: String,
    @SerialName("new_password") val newPassword: String,
)
