package com.xiaohelab.guard.android.data.mapper

import com.xiaohelab.guard.android.data.remote.dto.TaskDto
import com.xiaohelab.guard.android.data.remote.dto.TaskEventDto
import com.xiaohelab.guard.android.data.remote.dto.TrajectoryPointDto
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.domain.model.TaskEvent
import com.xiaohelab.guard.android.domain.model.TaskSource
import com.xiaohelab.guard.android.domain.model.TaskStatus
import com.xiaohelab.guard.android.domain.model.TrajectoryPoint

fun TaskDto.toDomain() = Task(
    id = id,
    patientId = patientId,
    patientNameMasked = patientNameMasked,
    status = when (status.uppercase()) {
        "ACTIVE" -> TaskStatus.ACTIVE
        "RESOLVED" -> TaskStatus.RESOLVED
        "FALSE_ALARM" -> TaskStatus.FALSE_ALARM
        else -> TaskStatus.UNKNOWN
    },
    source = when (source.uppercase()) {
        "NFC_SCAN" -> TaskSource.NFC_SCAN
        "MANUAL" -> TaskSource.MANUAL
        else -> TaskSource.UNKNOWN
    },
    startTime = startTime,
    latestEventTime = latestEventTime,
    remark = remark,
    posterUrl = posterUrl,
    version = version
)

fun TrajectoryPointDto.toDomain() = TrajectoryPoint(
    lat = lat,
    lng = lng,
    accuracy = accuracy,
    collectedAt = collectedAt,
    source = source
)

fun TaskEventDto.toDomain() = TaskEvent(
    eventId = eventId,
    taskId = taskId,
    type = type,
    operator = operator,
    description = description,
    occurredAt = occurredAt
)
