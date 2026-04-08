package com.xiaohelab.guard.android.domain.model

enum class ClueStatus {
    PENDING, LINKED, REJECTED, UNKNOWN
}

data class Clue(
    val id: String,
    val taskId: String?,
    val reporterType: String,
    val reporterMasked: String?,
    val description: String,
    val contactPhone: String?,
    val locationDesc: String?,
    val lat: Double?,
    val lng: Double?,
    val images: List<String>,
    val status: ClueStatus,
    val submittedAt: String
)

data class ResourceInfo(
    val resourceToken: String,
    val patientNameMasked: String,
    val taskId: String?
)
