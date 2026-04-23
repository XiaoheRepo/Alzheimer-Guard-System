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
class CreatePatientUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(name: String, gender: String?, birthDate: String?, medicalNotes: String?) =
        repo.create(PatientCreateRequest(name, gender, birthDate, medicalNotes))
}
class UpdatePatientProfileUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(id: String, name: String?, gender: String?, birthDate: String?, medicalNotes: String?, version: Long) =
        repo.updateProfile(id, PatientProfileUpdateRequest(name, gender, birthDate, medicalNotes, version))
}
class UpdateAppearanceUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(id: String, a: AppearanceDto, version: Long) =
        repo.updateAppearance(id, PatientAppearanceUpdateRequest(a.height, a.weight, a.features, a.photoUrls, version))

    suspend operator fun invoke(id: String, height: Int?, weight: Int?, features: String?, version: Long) =
        invoke(id, AppearanceDto(height, weight, features), version)
}
class SetFenceUseCase @Inject constructor(private val repo: ProfileRepository) {
    suspend operator fun invoke(id: String, f: FenceDto, version: Long) =
        repo.updateFence(id, PatientFenceUpdateRequest(f.centerLat, f.centerLng, f.radiusM, version))
}
class ArchivePatientUseCase @Inject constructor(private val repo: ProfileRepository) { suspend operator fun invoke(id: String) = repo.archive(id) }
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
