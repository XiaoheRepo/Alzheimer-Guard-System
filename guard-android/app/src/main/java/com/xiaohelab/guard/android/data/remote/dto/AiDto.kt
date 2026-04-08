package com.xiaohelab.guard.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiSessionDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    val title: String,
    val status: String,
    @SerialName("message_count") val messageCount: Int = 0,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AiMessageDto(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    val role: String,
    val content: String,
    val tokens: Int? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateSessionRequestDto(
    @SerialName("patient_id") val patientId: String,
    val title: String? = null
)

@Serializable
data class SendMessageRequestDto(
    val content: String
)

@Serializable
data class AiQuotaDto(
    val used: Int,
    val limit: Int,
    @SerialName("reset_at") val resetAt: String? = null
)

@Serializable
data class AiMemoryNoteDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    val content: String,
    val source: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateMemoryNoteRequestDto(
    @SerialName("patient_id") val patientId: String,
    val content: String
)
