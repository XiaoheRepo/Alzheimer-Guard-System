package com.xiaohelab.guard.android.data.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.network.safeApiCall
import com.xiaohelab.guard.android.core.common.map
import com.xiaohelab.guard.android.core.common.map
import com.xiaohelab.guard.android.data.mapper.toDomain
import com.xiaohelab.guard.android.data.remote.api.PatientApiService
import com.xiaohelab.guard.android.data.remote.dto.CreatePatientRequestDto
import com.xiaohelab.guard.android.data.remote.dto.GuardianInviteActionDto
import com.xiaohelab.guard.android.data.remote.dto.InviteGuardianRequestDto
import com.xiaohelab.guard.android.data.remote.dto.TransferOwnerRequestDto
import com.xiaohelab.guard.android.data.remote.dto.UpdateFenceRequestDto
import com.xiaohelab.guard.android.data.remote.dto.UpdatePatientRequestDto
import com.xiaohelab.guard.android.domain.model.GeoFence
import com.xiaohelab.guard.android.domain.model.Guardian
import com.xiaohelab.guard.android.domain.model.InviteGuardianRequest
import com.xiaohelab.guard.android.domain.model.Patient
import com.xiaohelab.guard.android.domain.repository.PatientRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepositoryImpl @Inject constructor(
    private val api: PatientApiService
) : PatientRepository {

    override suspend fun getMyPatients(): ApiResult<List<Patient>> =
        safeApiCall { api.getMyPatients() }.map { list -> list.map { it.toDomain() } }

    override suspend fun getPatientById(patientId: String): ApiResult<Patient> =
        safeApiCall { api.getPatientById(patientId) }.map { it.toDomain() }

    override suspend fun createPatient(
        name: String, age: Int?, gender: String?, height: Int?, weight: Int?,
        medicalHistory: String?, characteristics: String?
    ): ApiResult<Patient> = safeApiCall {
        api.createPatient(
            CreatePatientRequestDto(name, age, gender, height, weight, medicalHistory, characteristics)
        )
    }.map { it.toDomain() }

    override suspend fun updatePatient(
        patientId: String, name: String?, age: Int?, gender: String?, height: Int?,
        weight: Int?, medicalHistory: String?, characteristics: String?
    ): ApiResult<Patient> = safeApiCall {
        api.updatePatient(
            patientId,
            UpdatePatientRequestDto(name, age, gender, height, weight, medicalHistory, characteristics)
        )
    }.map { it.toDomain() }

    override suspend fun updateFence(patientId: String, fence: GeoFence): ApiResult<Unit> =
        safeApiCall {
            api.updateFence(
                patientId,
                UpdateFenceRequestDto(fence.centerLat, fence.centerLng, fence.radiusMeters, fence.enabled)
            )
        }

    override suspend fun getGuardians(patientId: String): ApiResult<List<Guardian>> =
        safeApiCall { api.getGuardians(patientId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun inviteGuardian(
        patientId: String, request: InviteGuardianRequest
    ): ApiResult<Unit> = safeApiCall {
        api.inviteGuardian(patientId, InviteGuardianRequestDto(request.phone, request.relation))
    }

    override suspend fun removeGuardian(patientId: String, guardianId: String): ApiResult<Unit> =
        safeApiCall { api.removeGuardian(patientId, guardianId) }

    override suspend fun transferOwner(patientId: String, toUserId: String): ApiResult<Unit> =
        safeApiCall { api.transferOwner(patientId, TransferOwnerRequestDto(toUserId)) }

    override suspend fun acceptGuardianInvite(inviteCode: String): ApiResult<Unit> =
        safeApiCall { api.acceptInvite(GuardianInviteActionDto(inviteCode)) }

    override suspend fun rejectGuardianInvite(inviteCode: String): ApiResult<Unit> =
        safeApiCall { api.rejectInvite(GuardianInviteActionDto(inviteCode)) }
}
