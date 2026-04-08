package com.xiaohelab.guard.android.domain.model

data class User(
    val id: String,
    val username: String,
    val role: String,
    val phone: String?,
    val avatar: String?,
    val createdAt: String?
)

data class LoginResult(
    val accessToken: String,
    val userId: String,
    val role: String
)
