package com.xiaohelab.guard.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val notificationId: String,
    val type: String,
    val title: String,
    val content: String,
    val readStatus: String,   // UNREAD / READ
    val createdAt: String,    // ISO-8601
    val relatedId: String? = null,
    val relatedType: String? = null
)
