package com.xiaohelab.guard.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String,
    val name: String,
    @SerialName("name_masked") val nameMasked: String,
    val age: Int? = null,
    val gender: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    @SerialName("medical_history") val medicalHistory: String? = null,
    val characteristics: String? = null,
    val avatar: String? = null,
    @SerialName("bound_tag_id") val boundTagId: String? = null,
    @SerialName("last_known_location") val lastKnownLocation: GeoPointDto? = null,
    val fence: GeoFenceDto? = null,
    val guardians: List<GuardianDto> = emptyList(),
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class GeoPointDto(
    val lat: Double,
    val lng: Double,
    val address: String? = null
)

@Serializable
data class GeoFenceDto(
    @SerialName("center_lat") val centerLat: Double,
    @SerialName("center_lng") val centerLng: Double,
    @SerialName("radius_meters") val radiusMeters: Int,
    val enabled: Boolean
)

@Serializable
data class GuardianDto(
    val id: String,
    val username: String,
    val phone: String? = null,
    val avatar: String? = null,
    val role: String,
    val relation: String? = null,
    val status: String
)

@Serializable
data class CreatePatientRequestDto(
    val name: String,
    val age: Int? = null,
    val gender: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    @SerialName("medical_history") val medicalHistory: String? = null,
    val characteristics: String? = null
)

@Serializable
data class UpdatePatientRequestDto(
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    @SerialName("medical_history") val medicalHistory: String? = null,
    val characteristics: String? = null
)

@Serializable
data class UpdateFenceRequestDto(
    @SerialName("center_lat") val centerLat: Double,
    @SerialName("center_lng") val centerLng: Double,
    @SerialName("radius_meters") val radiusMeters: Int,
    val enabled: Boolean
)

@Serializable
data class InviteGuardianRequestDto(
    val phone: String,
    val relation: String? = null
)

@Serializable
data class TransferOwnerRequestDto(
    @SerialName("to_user_id") val toUserId: String
)

@Serializable
data class GuardianInviteActionDto(
    @SerialName("invite_code") val inviteCode: String
)
