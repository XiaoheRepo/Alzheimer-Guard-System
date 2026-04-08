package com.xiaohelab.guard.android.domain.model

data class Patient(
    val id: String,
    val name: String,
    val nameMasked: String,
    val age: Int?,
    val gender: String?,
    val height: Int?,
    val weight: Int?,
    val medicalHistory: String?,
    val characteristics: String?,
    val avatar: String?,
    val boundTagId: String?,
    val lastKnownLocation: GeoPoint?,
    val fence: GeoFence?,
    val guardians: List<Guardian>,
    val createdAt: String
)

data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val address: String?
)

data class GeoFence(
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Int,
    val enabled: Boolean
)

data class Guardian(
    val id: String,
    val username: String,
    val phone: String?,
    val avatar: String?,
    val role: String,
    val relation: String?,
    val status: GuardianStatus
)

enum class GuardianStatus {
    ACTIVE, PENDING, UNKNOWN
}

data class InviteGuardianRequest(
    val phone: String,
    val relation: String?
)
