package com.xiaohelab.guard.android.data.remote.api

import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.core.network.dto.CursorPagedResponseDto
import com.xiaohelab.guard.android.data.remote.dto.NotificationDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponseDto<CursorPagedResponseDto<NotificationDto>>>

    @PUT("api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<ApiResponseDto<Unit>>

    @PUT("api/v1/notifications/read-all")
    suspend fun markAllRead(): Response<ApiResponseDto<Unit>>
}
