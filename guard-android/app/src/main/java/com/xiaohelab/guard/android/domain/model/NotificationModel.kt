package com.xiaohelab.guard.android.domain.model

data class Notification(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
    val relatedId: String?,
    val relatedType: String?
)
