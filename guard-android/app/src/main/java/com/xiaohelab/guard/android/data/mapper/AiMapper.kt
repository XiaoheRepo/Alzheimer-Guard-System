package com.xiaohelab.guard.android.data.mapper

import com.xiaohelab.guard.android.data.remote.dto.AiMemoryNoteDto
import com.xiaohelab.guard.android.data.remote.dto.AiMessageDto
import com.xiaohelab.guard.android.data.remote.dto.AiQuotaDto
import com.xiaohelab.guard.android.data.remote.dto.AiSessionDto
import com.xiaohelab.guard.android.domain.model.AiMemoryNote
import com.xiaohelab.guard.android.domain.model.AiMessage
import com.xiaohelab.guard.android.domain.model.AiQuota
import com.xiaohelab.guard.android.domain.model.AiSession
import com.xiaohelab.guard.android.domain.model.AiSessionStatus

fun AiSessionDto.toDomain() = AiSession(
    id = id,
    patientId = patientId,
    title = title,
    status = when (status.uppercase()) {
        "ACTIVE" -> AiSessionStatus.ACTIVE
        "ARCHIVED" -> AiSessionStatus.ARCHIVED
        else -> AiSessionStatus.UNKNOWN
    },
    messageCount = messageCount,
    lastMessageAt = lastMessageAt,
    createdAt = createdAt
)

fun AiMessageDto.toDomain() = AiMessage(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    tokens = tokens,
    createdAt = createdAt
)

fun AiQuotaDto.toDomain() = AiQuota(used = used, limit = limit, resetAt = resetAt)

fun AiMemoryNoteDto.toDomain() = AiMemoryNote(
    id = id,
    patientId = patientId,
    content = content,
    source = source,
    createdAt = createdAt
)
