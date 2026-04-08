package com.xiaohelab.guard.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_name_masked") val patientNameMasked: String,
    val status: String,
    val source: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("latest_event_time") val latestEventTime: String? = null,
    val remark: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    val version: Long = 0
)

@Serializable
data class CreateTaskRequestDto(
    @SerialName("patient_id") val patientId: String,
    val source: String,
    val remark: String? = null
)

@Serializable
data class CloseTaskRequestDto(
    val reason: String,
    val remarks: String? = null
)

@Serializable
data class TrajectoryPointDto(
    val lat: Double,
    val lng: Double,
    val accuracy: Float? = null,
    @SerialName("collected_at") val collectedAt: String,
    val source: String? = null
)

@Serializable
data class TaskEventDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("task_id") val taskId: String,
    val type: String,
    val operator: String? = null,
    val description: String,
    @SerialName("occurred_at") val occurredAt: String
)

@Serializable
data class WsTicketResponseDto(
    val ticket: String,
    @SerialName("expires_at") val expiresAt: String
)
