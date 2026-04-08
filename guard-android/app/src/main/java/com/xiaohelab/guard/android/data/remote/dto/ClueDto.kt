package com.xiaohelab.guard.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResourceInfoDto(
    @SerialName("resource_token") val resourceToken: String,
    @SerialName("patient_name_masked") val patientNameMasked: String,
    @SerialName("task_id") val taskId: String? = null
)

@Serializable
data class ManualClueRequestDto(
    @SerialName("anonymous_token") val anonymousToken: String,
    val name: String? = null,
    val phone: String? = null,
    val description: String,
    @SerialName("location_desc") val locationDesc: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val images: List<String> = emptyList()
)

@Serializable
data class ReportClueRequestDto(
    @SerialName("task_id") val taskId: String,
    val description: String,
    @SerialName("location_desc") val locationDesc: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    val images: List<String> = emptyList()
)

@Serializable
data class ClueDto(
    val id: String,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("reporter_type") val reporterType: String,
    @SerialName("reporter_masked") val reporterMasked: String? = null,
    val description: String,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("location_desc") val locationDesc: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val images: List<String> = emptyList(),
    val status: String,
    @SerialName("submitted_at") val submittedAt: String
)
