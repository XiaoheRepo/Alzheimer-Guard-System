package com.xiaohelab.guard.android.feature.profile.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** API V2.0 §3 /patients + /guardians endpoints. All IDs are String (HC-ID-String). */
interface PatientApi {

    @GET("/api/v1/patients")
    suspend fun list(): Response<ApiEnvelope<PatientListDto>>

    @POST("/api/v1/patients")
    suspend fun create(@Body body: PatientCreateRequest): Response<ApiEnvelope<PatientDto>>

    @GET("/api/v1/patients/{id}")
    suspend fun detail(@Path("id") id: String): Response<ApiEnvelope<PatientDto>>

    @PUT("/api/v1/patients/{id}/profile")
    suspend fun updateProfile(@Path("id") id: String, @Body body: PatientProfileUpdateRequest): Response<ApiEnvelope<PatientDto>>

    @PUT("/api/v1/patients/{id}/appearance")
    suspend fun updateAppearance(@Path("id") id: String, @Body body: PatientAppearanceUpdateRequest): Response<ApiEnvelope<PatientDto>>

    @PUT("/api/v1/patients/{id}/fence")
    suspend fun updateFence(@Path("id") id: String, @Body body: PatientFenceUpdateRequest): Response<ApiEnvelope<PatientDto>>

    @DELETE("/api/v1/patients/{id}")
    suspend fun archive(@Path("id") id: String): Response<ApiEnvelope<Unit>>

    // --- Guardians (§3 /guardians) ---
    @POST("/api/v1/patients/{id}/guardians/invitations")
    suspend fun invite(@Path("id") patientId: String, @Body body: GuardianInviteRequest): Response<ApiEnvelope<GuardianInvitationDto>>

    @POST("/api/v1/patients/{id}/guardians/invitations/{invite_id}/respond")
    suspend fun respondInvitation(
        @Path("id") patientId: String,
        @Path("invite_id") inviteId: String,
        @Body body: GuardianRespondRequest,
    ): Response<ApiEnvelope<Unit>>

    @POST("/api/v1/patients/{id}/guardians/primary-transfer")
    suspend fun initiateTransfer(@Path("id") patientId: String, @Body body: TransferInitiateRequest): Response<ApiEnvelope<TransferRequestDto>>

    @POST("/api/v1/patients/{id}/guardians/primary-transfer/{transfer_request_id}/respond")
    suspend fun respondTransfer(
        @Path("id") patientId: String,
        @Path("transfer_request_id") transferId: String,
        @Body body: TransferRespondRequest,
    ): Response<ApiEnvelope<Unit>>
}

@Serializable
data class PatientListDto(val items: List<PatientDto> = emptyList())

@Serializable
data class PatientDto(
    @SerialName("patient_id") val patientId: String,
    val name: String,
    val gender: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("medical_notes") val medicalNotes: String? = null,
    val appearance: AppearanceDto? = null,
    val fence: FenceDto? = null,
    val status: String? = null,
    @SerialName("version") val version: Long? = null,
)

@Serializable
data class AppearanceDto(
    val height: Int? = null,
    val weight: Int? = null,
    val features: String? = null,
    @SerialName("photo_urls") val photoUrls: List<String> = emptyList(),
)

@Serializable
data class FenceDto(
    @SerialName("center_lat") val centerLat: Double,
    @SerialName("center_lng") val centerLng: Double,
    @SerialName("radius_m") val radiusM: Int,
)

@Serializable
data class PatientCreateRequest(
    val name: String,
    val gender: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("medical_notes") val medicalNotes: String? = null,
)

@Serializable
data class PatientProfileUpdateRequest(
    val name: String? = null,
    val gender: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("medical_notes") val medicalNotes: String? = null,
    @SerialName("version") val version: Long,
)

@Serializable
data class PatientAppearanceUpdateRequest(
    val height: Int? = null,
    val weight: Int? = null,
    val features: String? = null,
    @SerialName("photo_urls") val photoUrls: List<String> = emptyList(),
    @SerialName("version") val version: Long,
)

@Serializable
data class PatientFenceUpdateRequest(
    @SerialName("center_lat") val centerLat: Double,
    @SerialName("center_lng") val centerLng: Double,
    @SerialName("radius_m") val radiusM: Int,
    @SerialName("version") val version: Long,
)

@Serializable
data class GuardianInviteRequest(
    @SerialName("invitee_identifier") val inviteeIdentifier: String,
    val relationship: String? = null,
    val role: String = "GUARDIAN",
)

@Serializable
data class GuardianInvitationDto(
    @SerialName("invite_id") val inviteId: String,
    @SerialName("patient_id") val patientId: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class GuardianRespondRequest(
    val accept: Boolean,
)

@Serializable
data class TransferInitiateRequest(
    @SerialName("target_user_id") val targetUserId: String,
    val reason: String? = null,
    @SerialName("version") val version: Long,
)

@Serializable
data class TransferRequestDto(
    @SerialName("transfer_request_id") val transferRequestId: String,
    @SerialName("patient_id") val patientId: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class TransferRespondRequest(
    val accept: Boolean,
)
