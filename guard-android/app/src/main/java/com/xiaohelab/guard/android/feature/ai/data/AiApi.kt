package com.xiaohelab.guard.android.feature.ai.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * M7 AI 域 API（REST 部分）。SSE 流式走 OkHttp EventSource。
 * HC-ID-String: session_id / intent_id 均为 String。
 * 错误码: E_AI_4041 / E_AI_4291 / E_AI_5001。
 */
interface AiSessionApi {
    @GET("/api/v1/ai/sessions")
    suspend fun listSessions(): Response<ApiEnvelope<AiSessionListDto>>

    @POST("/api/v1/ai/sessions")
    suspend fun createSession(
        @Body body: CreateSessionRequest,
    ): Response<ApiEnvelope<AiSessionDto>>

    @GET("/api/v1/ai/sessions/{session_id}")
    suspend fun getSession(
        @Path("session_id") sessionId: String,
    ): Response<ApiEnvelope<AiSessionDto>>

    @DELETE("/api/v1/ai/sessions/{session_id}")
    suspend fun deleteSession(
        @Path("session_id") sessionId: String,
    ): Response<ApiEnvelope<Unit>>

    @GET("/api/v1/ai/intents/{intent_id}")
    suspend fun getIntent(
        @Path("intent_id") intentId: String,
    ): Response<ApiEnvelope<IntentDto>>

    @POST("/api/v1/ai/intents/{intent_id}/confirm")
    suspend fun confirmIntent(
        @Path("intent_id") intentId: String,
    ): Response<ApiEnvelope<Unit>>

    @POST("/api/v1/ai/intents/{intent_id}/cancel")
    suspend fun cancelIntent(
        @Path("intent_id") intentId: String,
    ): Response<ApiEnvelope<Unit>>
}

/** HC-ID-String: session_id / patient_id 为 String。 */
@Serializable
data class AiSessionDto(
    @SerialName("session_id") val sessionId: String,
    @SerialName("patient_id") val patientId: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    val title: String? = null,
)

@Serializable
data class AiSessionListDto(
    val items: List<AiSessionDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class CreateSessionRequest(
    @SerialName("patient_id") val patientId: String? = null,
)

/** AI 对话请求体。mode 默认 CHAT，也可用 INTENT。 */
@Serializable
data class AiMessageRequest(
    val content: String,
    val mode: String = "CHAT",
)

/** SSE 数据块。done=true 时 delta 为 null，intent_id 非空表示需要确认。 */
@Serializable
data class AiMessageChunk(
    val delta: String? = null,
    val done: Boolean = false,
    @SerialName("intent_id") val intentId: String? = null,
)

/** HC-ID-String: intent_id 为 String。HC-02: status 只读。 */
@Serializable
data class IntentDto(
    @SerialName("intent_id") val intentId: String,
    val description: String,
    @SerialName("action_type") val actionType: String,
    /** HC-02: 状态由服务端维护，客户端只展示不推算。 */
    val status: String,
    val params: Map<String, String> = emptyMap(),
)
