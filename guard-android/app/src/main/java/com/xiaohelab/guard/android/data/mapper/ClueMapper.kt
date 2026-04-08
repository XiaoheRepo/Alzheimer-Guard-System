package com.xiaohelab.guard.android.data.mapper

import com.xiaohelab.guard.android.data.remote.dto.ClueDto
import com.xiaohelab.guard.android.data.remote.dto.ResourceInfoDto
import com.xiaohelab.guard.android.domain.model.Clue
import com.xiaohelab.guard.android.domain.model.ClueStatus
import com.xiaohelab.guard.android.domain.model.ResourceInfo

fun ResourceInfoDto.toDomain() = ResourceInfo(
    resourceToken = resourceToken,
    patientNameMasked = patientNameMasked,
    taskId = taskId
)

fun ClueDto.toDomain() = Clue(
    id = id,
    taskId = taskId,
    reporterType = reporterType,
    reporterMasked = reporterMasked,
    description = description,
    contactPhone = contactPhone,
    locationDesc = locationDesc,
    lat = lat,
    lng = lng,
    images = images,
    status = when (status.uppercase()) {
        "PENDING" -> ClueStatus.PENDING
        "LINKED" -> ClueStatus.LINKED
        "REJECTED" -> ClueStatus.REJECTED
        else -> ClueStatus.UNKNOWN
    },
    submittedAt = submittedAt
)
