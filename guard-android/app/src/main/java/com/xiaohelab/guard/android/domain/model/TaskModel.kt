package com.xiaohelab.guard.android.domain.model

enum class TaskStatus {
    ACTIVE, RESOLVED, FALSE_ALARM, UNKNOWN;

    fun isTerminal(): Boolean = this == RESOLVED || this == FALSE_ALARM
}

enum class TaskSource {
    NFC_SCAN, MANUAL, UNKNOWN
}

data class Task(
    val id: String,
    val patientId: String,
    val patientNameMasked: String,
    val status: TaskStatus,
    val source: TaskSource,
    val startTime: String,
    val latestEventTime: String?,
    val remark: String?,
    val posterUrl: String?,
    val version: Long
)

data class TaskEvent(
    val eventId: String,
    val taskId: String,
    val type: String,
    val operator: String?,
    val description: String,
    val occurredAt: String
)

data class TrajectoryPoint(
    val lat: Double,
    val lng: Double,
    val accuracy: Float?,
    val collectedAt: String,
    val source: String?
)

data class CloseTaskRequest(
    val reason: CloseReason,
    val remarks: String?
) {
    enum class CloseReason { FOUND, FALSE_ALARM }
}
