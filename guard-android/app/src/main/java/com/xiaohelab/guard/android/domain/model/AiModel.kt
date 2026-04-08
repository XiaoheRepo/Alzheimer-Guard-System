package com.xiaohelab.guard.android.domain.model

enum class AiSessionStatus {
    ACTIVE, ARCHIVED, UNKNOWN
}

data class AiSession(
    val id: String,
    val patientId: String,
    val title: String,
    val status: AiSessionStatus,
    val messageCount: Int,
    val lastMessageAt: String?,
    val createdAt: String
)

data class AiMessage(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val tokens: Int?,
    val createdAt: String
)

data class AiQuota(
    val used: Int,
    val limit: Int,
    val resetAt: String?
)

data class AiMemoryNote(
    val id: String,
    val patientId: String,
    val content: String,
    val source: String?,
    val createdAt: String
)
