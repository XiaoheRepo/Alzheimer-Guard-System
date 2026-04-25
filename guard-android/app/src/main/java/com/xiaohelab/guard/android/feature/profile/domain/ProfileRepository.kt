package com.xiaohelab.guard.android.feature.profile.domain

import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.common.map
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.profile.data.AppearanceDto
import com.xiaohelab.guard.android.feature.profile.data.FenceDto
import com.xiaohelab.guard.android.feature.profile.data.GuardianInvitationDto
import com.xiaohelab.guard.android.feature.profile.data.GuardianInviteRequest
import com.xiaohelab.guard.android.feature.profile.data.GuardianRespondRequest
import com.xiaohelab.guard.android.feature.profile.data.MissingPendingConfirmRequest
import com.xiaohelab.guard.android.feature.profile.data.PatientApi
import com.xiaohelab.guard.android.feature.profile.data.PatientAppearanceUpdateRequest
import com.xiaohelab.guard.android.feature.profile.data.PatientCreateRequest
import com.xiaohelab.guard.android.feature.profile.data.PatientDto
import com.xiaohelab.guard.android.feature.profile.data.PatientFenceUpdateRequest
import com.xiaohelab.guard.android.feature.profile.data.PatientListDto
import com.xiaohelab.guard.android.feature.profile.data.PatientProfileUpdateRequest
import com.xiaohelab.guard.android.feature.profile.data.TransferInitiateRequest
import com.xiaohelab.guard.android.feature.profile.data.TransferRequestDto
import com.xiaohelab.guard.android.feature.profile.data.TransferRespondRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject

interface ProfileRepository {
    suspend fun list(): MhResult<PatientListDto>
    suspend fun detail(id: String): MhResult<PatientDto>
    suspend fun create(req: PatientCreateRequest): MhResult<PatientDto>
    suspend fun updateProfile(id: String, req: PatientProfileUpdateRequest): MhResult<PatientDto>
    suspend fun updateAppearance(id: String, req: PatientAppearanceUpdateRequest): MhResult<PatientDto>
    suspend fun updateFence(id: String, req: PatientFenceUpdateRequest): MhResult<PatientDto>
    /** API V2.0 §3.3.5：CONFIRM_MISSING / CONFIRM_SAFE。 */
    suspend fun confirmMissingPending(id: String, action: String, remark: String? = null): MhResult<Unit>
    suspend fun archive(id: String): MhResult<Unit>
    suspend fun invite(patientId: String, req: GuardianInviteRequest): MhResult<GuardianInvitationDto>
    suspend fun respondInvitation(patientId: String, inviteId: String, accept: Boolean): MhResult<Unit>
    suspend fun initiateTransfer(patientId: String, req: TransferInitiateRequest): MhResult<TransferRequestDto>
    suspend fun respondTransfer(patientId: String, transferId: String, accept: Boolean): MhResult<Unit>
}

class ProfileRepositoryImpl @Inject constructor(
    private val api: PatientApi,
    private val json: Json,
) : ProfileRepository {
    override suspend fun list(): MhResult<PatientListDto> {
        return try {
            handleEnvelope { api.list() }.map { parsePatientList(it) }
        } catch (t: Throwable) {
            MhResult.Failure(DomainException(DomainException.CODE_PROTOCOL, "${t.javaClass.simpleName}: ${t.message}", cause = t))
        }
    }

    private fun parsePatientList(element: JsonElement): PatientListDto = when (element) {
        is JsonNull -> PatientListDto()
        is JsonArray -> PatientListDto(
            items = element.map { json.decodeFromJsonElement(it) },
            total = element.size,
        )
        is JsonObject -> json.decodeFromJsonElement(element)
        else -> PatientListDto()
    }
    override suspend fun detail(id: String) = handleEnvelope { api.detail(id) }
    override suspend fun create(req: PatientCreateRequest) = handleEnvelope { api.create(req) }
    override suspend fun updateProfile(id: String, req: PatientProfileUpdateRequest) = handleEnvelope { api.updateProfile(id, req) }
    override suspend fun updateAppearance(id: String, req: PatientAppearanceUpdateRequest) = handleEnvelope { api.updateAppearance(id, req) }
    override suspend fun updateFence(id: String, req: PatientFenceUpdateRequest) = handleEnvelope { api.updateFence(id, req) }
    override suspend fun confirmMissingPending(id: String, action: String, remark: String?) =
        when (val r = handleEnvelope {
            api.confirmMissingPending(
                id,
                MissingPendingConfirmRequest(action = action, remark = remark),
            )
        }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
    override suspend fun archive(id: String) = when (val r = handleEnvelope { api.archive(id) }) {
        is MhResult.Success -> MhResult.Success(Unit, r.trace)
        is MhResult.Failure -> r
    }
    override suspend fun invite(patientId: String, req: GuardianInviteRequest) = handleEnvelope { api.invite(patientId, req) }
    override suspend fun respondInvitation(patientId: String, inviteId: String, accept: Boolean) =
        when (val r = handleEnvelope { api.respondInvitation(patientId, inviteId, GuardianRespondRequest(accept)) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
    override suspend fun initiateTransfer(patientId: String, req: TransferInitiateRequest) = handleEnvelope { api.initiateTransfer(patientId, req) }
    override suspend fun respondTransfer(patientId: String, transferId: String, accept: Boolean) =
        when (val r = handleEnvelope { api.respondTransfer(patientId, transferId, TransferRespondRequest(accept)) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
}

// --- UseCases (domain API) ---
class ListPatientsUseCase @Inject constructor(private val repo: ProfileRepository) { suspend operator fun invoke() = repo.list() }
class GetPatientDetailUseCase @Inject constructor(private val repo: ProfileRepository) { suspend operator fun invoke(id: String) = repo.detail(id) }

/**
 * 创建患者档案。
 *
 * 对齐后端 PatientCreateRequest：`name` / `gender` / `birthday` / `avatar_url` 均必填，
 * 其余慢病、过敏、外观、紧急联系人均可选。
 *
 * 旧签名（仅 name/gender/birthDate/medicalNotes）已废弃；UI 现在必须采集 avatar_url 与拆分的
 * 慢病/用药/过敏字段（chronic_diseases / medication / allergy）。
 */
class CreatePatientUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(
        name: String,
        gender: String,
        birthday: String,
        avatarUrl: String,
        chronicDiseases: String? = null,
        medication: String? = null,
        allergy: String? = null,
        emergencyContactPhone: String? = null,
        longTextProfile: String? = null,
        appearanceHeightCm: Int? = null,
        appearanceWeightKg: Int? = null,
        appearanceClothing: String? = null,
        appearanceFeatures: String? = null,
    ) = repo.create(
        PatientCreateRequest(
            patientName = name,
            gender = gender,
            birthday = birthday,
            avatarUrl = avatarUrl,
            chronicDiseases = chronicDiseases,
            medication = medication,
            allergy = allergy,
            emergencyContactPhone = emergencyContactPhone,
            longTextProfile = longTextProfile,
            appearance = if (
                appearanceHeightCm != null || appearanceWeightKg != null ||
                appearanceClothing != null || appearanceFeatures != null
            ) AppearanceDto(
                heightCm = appearanceHeightCm,
                weightKg = appearanceWeightKg,
                clothing = appearanceClothing,
                features = appearanceFeatures,
            ) else null,
        )
    )
}

class UpdatePatientProfileUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(
        id: String,
        name: String? = null,
        gender: String? = null,
        birthday: String? = null,
        avatarUrl: String? = null,
        chronicDiseases: String? = null,
        medication: String? = null,
        allergy: String? = null,
        emergencyContactPhone: String? = null,
        longTextProfile: String? = null,
    ) = repo.updateProfile(
        id,
        PatientProfileUpdateRequest(
            patientName = name,
            gender = gender,
            birthday = birthday,
            avatarUrl = avatarUrl,
            chronicDiseases = chronicDiseases,
            medication = medication,
            allergy = allergy,
            emergencyContactPhone = emergencyContactPhone,
            longTextProfile = longTextProfile,
        ),
    )
}

class UpdateAppearanceUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(
        id: String,
        heightCm: Int? = null,
        weightKg: Int? = null,
        clothing: String? = null,
        features: String? = null,
    ) = repo.updateAppearance(
        id,
        PatientAppearanceUpdateRequest(
            heightCm = heightCm,
            weightKg = weightKg,
            clothing = clothing,
            features = features,
        ),
    )

    suspend operator fun invoke(id: String, a: AppearanceDto) =
        invoke(id, a.heightCm, a.weightKg, a.clothing, a.features)
}

class SetFenceUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(id: String, f: FenceDto) =
        repo.updateFence(
            id,
            // API V2.0 §3.3.4：请求体为嵌套 fence{}。FenceDto 直接复用 wire DTO。
            PatientFenceUpdateRequest(
                fence = FenceDto(
                    enabled = f.enabled,
                    centerLat = f.centerLat,
                    centerLng = f.centerLng,
                    radiusM = f.radiusM,
                    coordSystem = f.coordSystem ?: "WGS84",
                ),
            ),
        )

    /** 关闭围栏（HC-Coord 兼容；其他参数置空）。 */
    suspend fun disable(id: String) =
        repo.updateFence(id, PatientFenceUpdateRequest(fence = FenceDto(enabled = false)))
}
class ArchivePatientUseCase @Inject constructor(private val repo: ProfileRepository) { suspend operator fun invoke(id: String) = repo.archive(id) }

/** 走失/安全确认（API V2.0 §3.3.5）。action ∈ {CONFIRM_MISSING, CONFIRM_SAFE}。 */
class ConfirmMissingPendingUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(patientId: String, action: String, remark: String? = null) =
        repo.confirmMissingPending(patientId, action, remark)
}
class InviteGuardianUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(patientId: String, identifier: String, relationship: String?) =
        repo.invite(patientId, GuardianInviteRequest(identifier, relationship))
}
class RespondInvitationUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(patientId: String, inviteId: String, accept: Boolean) = repo.respondInvitation(patientId, inviteId, accept)
}
class InitiatePrimaryTransferUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(patientId: String, targetUserId: String, reason: String?, version: Long) =
        repo.initiateTransfer(patientId, TransferInitiateRequest(targetUserId, reason, version))
}
class RespondPrimaryTransferUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(patientId: String, transferId: String, accept: Boolean) = repo.respondTransfer(patientId, transferId, accept)
}
