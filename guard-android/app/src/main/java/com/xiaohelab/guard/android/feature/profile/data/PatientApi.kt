package com.xiaohelab.guard.android.feature.profile.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import com.xiaohelab.guard.android.core.network.IdAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * API V2.0 §3.3 /patients endpoints。
 *
 * **HC-ID-String**：客户端所有 patient_id 字段语义为 String；当前后端仍以 Long 序列化为 JSON number，
 * 已通过 [IdAsStringSerializer] 兼容（参见 RFC: 后端 IdAsStringJackson 模块化）。
 *
 * **wire 格式契约**：与后端 `PatientCreateRequest` / `PatientResponse` 实际字段（snake_case）严格对齐。
 * 注意后端目前**未采用** API V2.0 §3.3.1 描述的 `appearance{}` / `fence{}` 嵌套结构，而使用扁平字段
 * `appearance_height_cm` / `fence_center_lat` 等；本端按后端实际实现对齐（基线偏离已记录在
 * 「后端 wire 与 API V2.0 偏差」RFC 中）。
 */
interface PatientApi {

    @GET("/api/v1/patients")
    suspend fun list(): Response<ApiEnvelope<JsonElement>>

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

    /** API V2.0 §3.3.5 走失/安全确认。 */
    @POST("/api/v1/patients/{id}/missing-pending/confirm")
    suspend fun confirmMissingPending(
        @Path("id") id: String,
        @Body body: MissingPendingConfirmRequest,
    ): Response<ApiEnvelope<JsonElement>>

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
data class PatientListDto(
    val items: List<PatientDto> = emptyList(),
    @SerialName("page_no") val pageNo: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val total: Int = 0,
    @SerialName("has_next") val hasNext: Boolean = false,
)

/**
 * 患者档案响应 DTO，对齐后端 `PatientResponse` 实际 wire 字段。
 *
 * - `name` 字段：后端 `PatientResponse.name`（API V2.0 文档使用 `patient_name`，后端尚未对齐 → RFC）。
 * - 外观/围栏：后端使用扁平字段 `appearance_*` / `fence_*`，非嵌套对象。
 * - 状态：使用 `lost_status`（NORMAL / MISSING_PENDING / MISSING）。
 * - 版本：使用 `profile_version`（与后端乐观锁字段对齐）。
 * - 电话：后端只下发脱敏后的 `emergency_contact_phone_masked`。
 */
@Serializable
data class PatientDto(
    @Serializable(with = IdAsStringSerializer::class)
    @SerialName("patient_id")
    val patientId: String,
    /** 当前后端字段为 `name`；同时容忍 `patient_name` 兼容文档版本。 */
    @SerialName("name") val name: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    val gender: String? = null,
    /** ISO yyyy-MM-dd，对齐后端 `birthday`。 */
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("short_code") val shortCode: String? = null,
    @SerialName("profile_no") val profileNo: String? = null,
    @SerialName("chronic_diseases") val chronicDiseases: String? = null,
    val medication: String? = null,
    val allergy: String? = null,
    @SerialName("emergency_contact_phone_masked") val emergencyContactPhoneMasked: String? = null,
    @SerialName("long_text_profile") val longTextProfile: String? = null,
    // 外观（扁平）
    @SerialName("appearance_height_cm") val appearanceHeightCm: Int? = null,
    @SerialName("appearance_weight_kg") val appearanceWeightKg: Int? = null,
    @SerialName("appearance_clothing") val appearanceClothing: String? = null,
    @SerialName("appearance_features") val appearanceFeatures: String? = null,
    // 围栏（扁平）
    @SerialName("fence_enabled") val fenceEnabled: Boolean? = null,
    @SerialName("fence_center_lat") val fenceCenterLat: Double? = null,
    @SerialName("fence_center_lng") val fenceCenterLng: Double? = null,
    @SerialName("fence_radius_m") val fenceRadiusM: Int? = null,
    @SerialName("fence_coord_system") val fenceCoordSystem: String? = null,
    /** API V2.0：NORMAL / MISSING_PENDING / MISSING。HC-02 客户端只读。 */
    @SerialName("lost_status") val lostStatus: String? = null,
    @SerialName("profile_version") val profileVersion: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    /** 列表聚合字段（监护关系角色），详情接口可能不返回。 */
    @SerialName("relation_role") val relationRole: String? = null,
    val age: Int? = null,
) {
    /** 兼容字段优先级：`name` > `patient_name`（API V2.0 字典）。 */
    val displayName: String get() = name ?: patientName ?: ""
}

/**
 * 创建患者请求体，对齐后端 `PatientCreateRequest`。
 *
 * 必填：`name` / `gender` / `birthday` / `avatar_url`。
 */
@Serializable
data class PatientCreateRequest(
    val name: String,
    /** MALE / FEMALE / UNKNOWN。 */
    val gender: String,
    /** yyyy-MM-dd。 */
    @SerialName("birthday") val birthday: String,
    /** OSS URL，必填且禁止空值。 */
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("chronic_diseases") val chronicDiseases: String? = null,
    val medication: String? = null,
    val allergy: String? = null,
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerialName("long_text_profile") val longTextProfile: String? = null,
    @SerialName("appearance_height_cm") val appearanceHeightCm: Int? = null,
    @SerialName("appearance_weight_kg") val appearanceWeightKg: Int? = null,
    @SerialName("appearance_clothing") val appearanceClothing: String? = null,
    @SerialName("appearance_features") val appearanceFeatures: String? = null,
)

/**
 * 更新基础档案请求体，对齐后端 `PatientUpdateRequest`（PUT /{id}/profile）。
 *
 * 后端不接 `version` 字段（乐观锁在 service 层基于 `profile_version` 自校验），所以不在请求体声明。
 * 头像 URL 可选但**不允许清空**，赋空字符串将被后端拒绝（E_PRO_4014）。
 */
@Serializable
data class PatientProfileUpdateRequest(
    val name: String? = null,
    val gender: String? = null,
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("chronic_diseases") val chronicDiseases: String? = null,
    val medication: String? = null,
    val allergy: String? = null,
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerialName("long_text_profile") val longTextProfile: String? = null,
    @SerialName("appearance_height_cm") val appearanceHeightCm: Int? = null,
    @SerialName("appearance_weight_kg") val appearanceWeightKg: Int? = null,
    @SerialName("appearance_clothing") val appearanceClothing: String? = null,
    @SerialName("appearance_features") val appearanceFeatures: String? = null,
)

/** 外观更新请求，对齐后端 `AppearanceUpdateRequest`。字段名 `height_cm` / `weight_kg`，无 photo_urls / version。 */
@Serializable
data class PatientAppearanceUpdateRequest(
    @SerialName("height_cm") val heightCm: Int? = null,
    @SerialName("weight_kg") val weightKg: Int? = null,
    val clothing: String? = null,
    val features: String? = null,
)

/**
 * 围栏更新请求，对齐后端 `FenceUpdateRequest`。
 * 字段名 `fence_*` 全前缀；`fence_enabled` 必填；启用时需带 lat/lng/radius。
 */
@Serializable
data class PatientFenceUpdateRequest(
    @SerialName("fence_enabled") val fenceEnabled: Boolean,
    @SerialName("fence_center_lat") val fenceCenterLat: Double? = null,
    @SerialName("fence_center_lng") val fenceCenterLng: Double? = null,
    @SerialName("fence_radius_m") val fenceRadiusM: Int? = null,
    /** WGS84 / GCJ-02 / BD-09，默认 WGS84。 */
    @SerialName("fence_coord_system") val fenceCoordSystem: String? = "WGS84",
)

/** API V2.0 §3.3.5 走失/安全确认请求体。 */
@Serializable
data class MissingPendingConfirmRequest(
    /** CONFIRM_MISSING / CONFIRM_SAFE。 */
    val action: String,
    val remark: String? = null,
    @SerialName("request_time") val requestTime: String? = null,
)

// ─── 本地 UI 模型（非序列化）────────────────────────────────────────────
// 注意：后端 wire 不存在嵌套 appearance / fence 对象。下面的两个类**仅供 UI 状态聚合使用**，
// 不应再作为请求/响应字段被序列化（已从 PatientDto 中移除嵌套引用）。

/** UI 聚合：外观信息。 */
data class AppearanceDto(
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val clothing: String? = null,
    val features: String? = null,
)

/** UI 聚合：围栏信息。 */
data class FenceDto(
    val enabled: Boolean,
    val centerLat: Double?,
    val centerLng: Double?,
    val radiusM: Int?,
    val coordSystem: String? = "WGS84",
)

@Serializable
data class GuardianInviteRequest(
    @SerialName("invitee_identifier") val inviteeIdentifier: String,
    val relationship: String? = null,
    val role: String = "GUARDIAN",
)

@Serializable
data class GuardianInvitationDto(
    @Serializable(with = IdAsStringSerializer::class)
    @SerialName("invite_id") val inviteId: String,
    @Serializable(with = IdAsStringSerializer::class)
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
    @Serializable(with = IdAsStringSerializer::class)
    @SerialName("transfer_request_id") val transferRequestId: String,
    @Serializable(with = IdAsStringSerializer::class)
    @SerialName("patient_id") val patientId: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class TransferRespondRequest(
    val accept: Boolean,
)
