package com.xiaohelab.guard.android.data.remote.api

import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.data.remote.dto.ChangePasswordRequestDto
import com.xiaohelab.guard.android.data.remote.dto.LoginRequestDto
import com.xiaohelab.guard.android.data.remote.dto.LoginResponseDto
import com.xiaohelab.guard.android.data.remote.dto.RegisterRequestDto
import com.xiaohelab.guard.android.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponseDto<LoginResponseDto>>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<ApiResponseDto<UserDto>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<ApiResponseDto<Unit>>

    @PUT("api/v1/users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): Response<ApiResponseDto<Unit>>

    @GET("api/v1/users/me")
    suspend fun getMe(): Response<ApiResponseDto<UserDto>>
}
