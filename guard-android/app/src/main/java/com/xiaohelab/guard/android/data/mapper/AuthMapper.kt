package com.xiaohelab.guard.android.data.mapper

import com.xiaohelab.guard.android.data.remote.dto.LoginResponseDto
import com.xiaohelab.guard.android.data.remote.dto.UserDto
import com.xiaohelab.guard.android.domain.model.LoginResult
import com.xiaohelab.guard.android.domain.model.User

fun LoginResponseDto.toDomain() = LoginResult(
    accessToken = accessToken,
    userId = userId,
    role = role
)

fun UserDto.toDomain() = User(
    id = id,
    username = username,
    role = role,
    phone = phone,
    avatar = avatar,
    createdAt = createdAt
)
