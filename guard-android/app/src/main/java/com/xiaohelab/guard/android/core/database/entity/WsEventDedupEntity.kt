package com.xiaohelab.guard.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 已消费 WebSocket 事件 ID 去重表（HandBook §11.2） */
@Entity(tableName = "ws_event_dedup")
data class WsEventDedupEntity(
    @PrimaryKey val eventId: String,
    val consumedAt: Long = System.currentTimeMillis()
)
