package com.xiaohelab.guard.android.data.remote.api

import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.data.remote.dto.ClueDto
import com.xiaohelab.guard.android.data.remote.dto.ManualClueRequestDto
import com.xiaohelab.guard.android.data.remote.dto.ReportClueRequestDto
import com.xiaohelab.guard.android.data.remote.dto.ResourceInfoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ClueApiService {
    /** 解析 NFC/二维码资源令牌（§3.4） */
    @GET("r/{resource_token}")
    suspend fun resolveResourceToken(
        @Path("resource_token") token: String
    ): Response<ApiResponseDto<ResourceInfoDto>>

    /** 手动入口 — 匿名上报（§3.5） */
    @POST("api/v1/public/clues/manual-entry")
    suspend fun submitManualClue(
        @Body request: ManualClueRequestDto
    ): Response<ApiResponseDto<ClueDto>>

    /** 已登录用户上报线索（§3.6） */
    @POST("api/v1/clues/report")
    suspend fun reportClue(
        @Body request: ReportClueRequestDto
    ): Response<ApiResponseDto<ClueDto>>

    /** 线索详情 */
    @GET("api/v1/clues/{clue_id}")
    suspend fun getClueById(
        @Path("clue_id") clueId: String
    ): Response<ApiResponseDto<ClueDto>>
}
