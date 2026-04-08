package com.xiaohelab.guard.android.data.mapper

import com.xiaohelab.guard.android.data.remote.dto.GeoFenceDto
import com.xiaohelab.guard.android.data.remote.dto.GeoPointDto
import com.xiaohelab.guard.android.data.remote.dto.GuardianDto
import com.xiaohelab.guard.android.data.remote.dto.PatientDto
import com.xiaohelab.guard.android.domain.model.GeoFence
import com.xiaohelab.guard.android.domain.model.GeoPoint
import com.xiaohelab.guard.android.domain.model.Guardian
import com.xiaohelab.guard.android.domain.model.GuardianStatus
import com.xiaohelab.guard.android.domain.model.Patient

fun PatientDto.toDomain() = Patient(
    id = id,
    name = name,
    nameMasked = nameMasked,
    age = age,
    gender = gender,
    height = height,
    weight = weight,
    medicalHistory = medicalHistory,
    characteristics = characteristics,
    avatar = avatar,
    boundTagId = boundTagId,
    lastKnownLocation = lastKnownLocation?.toDomain(),
    fence = fence?.toDomain(),
    guardians = guardians.map { it.toDomain() },
    createdAt = createdAt
)

fun GeoPointDto.toDomain() = GeoPoint(lat = lat, lng = lng, address = address)

fun GeoFenceDto.toDomain() = GeoFence(
    centerLat = centerLat,
    centerLng = centerLng,
    radiusMeters = radiusMeters,
    enabled = enabled
)

fun GuardianDto.toDomain() = Guardian(
    id = id,
    username = username,
    phone = phone,
    avatar = avatar,
    role = role,
    relation = relation,
    status = when (status.uppercase()) {
        "ACTIVE" -> GuardianStatus.ACTIVE
        "PENDING" -> GuardianStatus.PENDING
        else -> GuardianStatus.UNKNOWN
    }
)
