package com.xiaohelab.guard.android.data.mapper

import com.xiaohelab.guard.android.data.remote.dto.NotificationDto
import com.xiaohelab.guard.android.domain.model.Notification

fun NotificationDto.toDomain() = Notification(
    id = id,
    type = type,
    title = title,
    content = content,
    isRead = isRead,
    createdAt = createdAt,
    relatedId = relatedId,
    relatedType = relatedType
)
