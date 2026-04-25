package com.xiaohelab.guard.android.feature.ai.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import com.xiaohelab.guard.android.core.network.IdAsStringSerializer
import com.xiaohelab.guard.android.core.network.NullableIdAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * M7 AI 域 API（REST 部分）。SSE 流式走 OkHttp EventSource，详见 AiUi.sseStream()。
 *
 * **HC-ID-String**：session_id / intent_id 均为 String；后端当前以 Long 序列化数值 ID
 * （patient_id / task_id），通过 [IdAsStringSerializer] / [NullableIdAsStringSerializer] 兼容。
 *
 * 错误码：E_AI_4001 / E_AI_4002 / E_AI_4031 / E_AI_4033 / E_AI_4041 / E_AI_4091 /
 * E_AI_4292 / E_AI_4293 / E_AI_5021 / E_AI_5031（参见 ErrorMessageMapper）。
 */
interface AiSessionApi {
    @GET("/api/v1/ai/sessions")
    suspend fun listSessions(
        @Query("patient_id") patientId: String? = null,
        @Query("task_id") taskId: String? = null,
        @Query("status") status: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("page_size") pageSize: Int = 20,
    ): Response<ApiEnvelope<AiSessionListDto>>

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
    ): Response<ApiEnvelope<AiSessionDto>>

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

/** 会话实体。后端字段较多，这里只声明客户端关心的；其它字段由 `ignoreUnknownKeys=true` 容忍。 */
@Serializable
data class AiSessionDto(
    @Serializable(with = IdAsStringSerializer::class)
    @SerialName("session_id") val sessionId: String,
    @Serializable(with = NullableIdAsStringSerializer::class)
    @SerialName("patient_id") val patientId: String? = null,
    @Serializable(with = NullableIdAsStringSerializer::class)
    @SerialName("task_id") val taskId: String? = null,
    /** ACTIVE / ARCHIVED；后端可能不下发，故可空。 */
    val status: String? = null,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
)

/**
 * 会话列表（Cursor 分页），对齐后端 `CursorResponse` 字段。
 *
 * 兼容：当后端返回 Offset 分页（含 `total` / `page_no`）时，相关字段为 null 不影响解析。
 */
@Serializable
data class AiSessionListDto(
    val items: List<AiSessionDto> = emptyList(),
    @SerialName("page_size") val pageSize: Int? = null,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_next") val hasNext: Boolean? = null,
    val total: Int? = null,
)

/**
 * 创建会话请求体（API V2.0 §3.5.1）。
 *
 * **基线裁决**：`patient_id` / `task_id` 均为必填字符串。AI 会话必须挂在某条寻回任务上下文之下。
 * UI 入口必须在已有 task 的语境中创建会话；无任务上下文（如健康咨询）当前不允许直接进入 AI 会话。
 */
@Serializable
data class CreateSessionRequest(
    @SerialName("patient_id") val patientId: String,
    @SerialName("task_id") val taskId: String,
    /** 可选首条用户提示词（部分后端版本支持）。 */
    val prompt: String? = null,
)

/**
 * AI 对话请求体。
 *
 * 字段名 `prompt` 对齐后端 `AiChatRequest.prompt`（API V2.0 §3.5.2 / 后端 SSE 端点同步）。
 * 历史代码使用 `content` / `mode` 是错误的，已废弃。
 */
@Serializable
data class AiMessageRequest(
    val prompt: String,
)

// ─── SSE 事件载荷 ─────────────────────────────────────────────────────────────
//
// 后端 SseEmitter 按事件名分发：`token` / `usage` / `done` / `error`（API V2.0 §3.5.2 还规划了
// `tool_call`，后端目前未发）。每种事件的 `data:` JSON 结构不同，**不能**用统一 schema 解析。
// 客户端在 EventSourceListener.onEvent(type, data) 内按 `type` 调用对应反序列化。

/** `event: token` 载荷：AI 逐 token 输出。 */
@Serializable
data class AiTokenEvent(
    /** 增量文本切片。 */
    val content: String,
    val index: Int = 0,
)

/** `event: usage` 载荷：Token 消耗统计。 */
@Serializable
data class AiUsageEvent(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("billing_source") val billingSource: String? = null,
)

/** `event: done` 载荷：流结束。 */
@Serializable
data class AiDoneEvent(
    @SerialName("finish_reason") val finishReason: String = "stop",
)

/** `event: error` 载荷：错误信息。 */
@Serializable
data class AiErrorEvent(
    val code: String,
    val message: String? = null,
)

/**
 * `event: tool_call` 载荷（API V2.0 §3.5.2 计划字段；后端尚未发出）。
 * 客户端解析后通常需配合 GET /ai/intents/{intent_id} 取详情。
 */
@Serializable
data class AiToolCallEvent(
    @Serializable(with = IdAsStringSerializer::class)
    @SerialName("intent_id") val intentId: String,
    val action: String? = null,
    val description: String? = null,
    @SerialName("execution_level") val executionLevel: String? = null,
    @SerialName("requires_confirm") val requiresConfirm: Boolean? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

/** Intent DTO；HC-02：status 由服务端维护，客户端只读不推算。 */
@Serializable
data class IntentDto(
    @Serializable(with = IdAsStringSerializer::class)
    @SerialName("intent_id") val intentId: String,
    val description: String,
    @SerialName("action_type") val actionType: String,
    val status: String,
    val params: Map<String, String> = emptyMap(),
)

