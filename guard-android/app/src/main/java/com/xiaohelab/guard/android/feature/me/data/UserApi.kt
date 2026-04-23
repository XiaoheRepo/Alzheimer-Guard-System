package com.xiaohelab.guard.android.feature.me.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import com.xiaohelab.guard.android.feature.auth.data.UserProfileDto
import retrofit2.Response
import retrofit2.http.GET

interface UserApi {
    @GET("/api/v1/users/me")
    suspend fun me(): Response<ApiEnvelope<UserProfileDto>>
}
