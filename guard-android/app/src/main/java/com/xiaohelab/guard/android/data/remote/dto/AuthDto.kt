package com.xiaohelab.guard.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val username: String,
    val password: String,
    val phone: String?,
    val role: String
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("old_password") val oldPassword: String,
    @SerialName("new_password") val newPassword: String
)

@Serializable
data class LoginResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: String,
    val role: String
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val role: String,
    val phone: String? = null,
    val avatar: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
