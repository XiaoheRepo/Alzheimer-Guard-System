package com.xiaohelab.guard.android.data.remote.api

import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.data.remote.dto.AiMemoryNoteDto
import com.xiaohelab.guard.android.data.remote.dto.AiMessageDto
import com.xiaohelab.guard.android.data.remote.dto.AiQuotaDto
import com.xiaohelab.guard.android.data.remote.dto.AiSessionDto
import com.xiaohelab.guard.android.data.remote.dto.CreateMemoryNoteRequestDto
import com.xiaohelab.guard.android.data.remote.dto.CreateSessionRequestDto
import com.xiaohelab.guard.android.data.remote.dto.SendMessageRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AiApiService {
    @GET("api/v1/ai/sessions")
    suspend fun getSessions(
        @Query("patient_id") patientId: String
    ): Response<ApiResponseDto<List<AiSessionDto>>>

    @POST("api/v1/ai/sessions")
    suspend fun createSession(@Body request: CreateSessionRequestDto): Response<ApiResponseDto<AiSessionDto>>

    @GET("api/v1/ai/sessions/{id}/messages")
    suspend fun getMessages(@Path("id") sessionId: String): Response<ApiResponseDto<List<AiMessageDto>>>

    /** SSE 流式消息 — 返回 ResponseBody 由调用方逐行读取 */
    @Streaming
    @POST("api/v1/ai/sessions/{id}/messages")
    suspend fun sendMessageStream(
        @Path("id") sessionId: String,
        @Body request: SendMessageRequestDto
    ): Response<ResponseBody>

    @GET("api/v1/ai/quota")
    suspend fun getQuota(): Response<ApiResponseDto<AiQuotaDto>>

    @GET("api/v1/ai/memory-notes")
    suspend fun getMemoryNotes(
        @Query("patient_id") patientId: String
    ): Response<ApiResponseDto<List<AiMemoryNoteDto>>>

    @POST("api/v1/ai/memory-notes")
    suspend fun createMemoryNote(@Body request: CreateMemoryNoteRequestDto): Response<ApiResponseDto<AiMemoryNoteDto>>

    @DELETE("api/v1/ai/memory-notes/{id}")
    suspend fun deleteMemoryNote(@Path("id") noteId: String): Response<ApiResponseDto<Unit>>
}
