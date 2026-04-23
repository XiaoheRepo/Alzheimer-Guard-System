package com.xiaohelab.guard.android.feature.auth.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * API V2.0 §2 /auth endpoints.  Full signatures aligned with API spec.
 * All ID fields kept as String (HC-ID-String).
 */
interface AuthApi {
    @POST("/api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<ApiEnvelope<RegisterResponseDto>>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<LoginResponseDto>>

    @POST("/api/v1/auth/token/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<ApiEnvelope<RefreshResponseDto>>

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<ApiEnvelope<Unit>>

    @POST("/api/v1/auth/password-reset/request")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequest): Response<ApiEnvelope<Unit>>

    @POST("/api/v1/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body body: PasswordResetConfirm): Response<ApiEnvelope<Unit>>

    /** API V2.0 §3.6.6: 获取当前登录用户信息. auth is "Bearer <access_token>". */
    @GET("/api/v1/users/me")
    suspend fun getMe(@Header("Authorization") auth: String): Response<ApiEnvelope<UserProfileDto>>
}

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val nickname: String? = null,
    val role: String = "FAMILY",
)

@Serializable
data class RegisterResponseDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    // Server returns email_verification_sent per API §3.6.1; extra fields below are optional.
    @SerialName("email_verification_sent") val emailVerificationSent: Boolean? = null,
    val email: String? = null,
    val role: String? = null,
    val nickname: String? = null,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class UserProfileDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    val nickname: String? = null,
    val role: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null,
)

@Serializable
data class LoginResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long,
    // Optional: some server builds omit the user object; we call /users/me in that case.
    val user: UserProfileDto? = null,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class LogoutRequest(
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("request_time") val requestTime: String, // ISO-8601
)

@Serializable
data class PasswordResetRequest(
    val email: String,
    val locale: String? = null,
)

@Serializable
data class PasswordResetConfirm(
    val token: String,
    @SerialName("new_password") val newPassword: String,
)
