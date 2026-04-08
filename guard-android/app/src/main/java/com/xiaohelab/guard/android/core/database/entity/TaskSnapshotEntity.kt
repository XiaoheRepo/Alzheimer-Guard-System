package com.xiaohelab.guard.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 任务快照缓存（cache-aside，防止 WebSocket 事件版本乱序） */
@Entity(tableName = "task_snapshots")
data class TaskSnapshotEntity(
    @PrimaryKey val taskId: String,
    val patientId: String,
    val patientNameMasked: String,
    val status: String,            // ACTIVE / RESOLVED / FALSE_ALARM
    val source: String,
    val version: Long,
    val startTime: String,
    val latestEventTime: String,
    val remark: String? = null,
    val posterUrl: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
