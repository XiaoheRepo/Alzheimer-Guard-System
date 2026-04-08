package com.xiaohelab.guard.android.domain.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.GeoFence
import com.xiaohelab.guard.android.domain.model.Guardian
import com.xiaohelab.guard.android.domain.model.InviteGuardianRequest
import com.xiaohelab.guard.android.domain.model.Patient

interface PatientRepository {
    suspend fun getMyPatients(): ApiResult<List<Patient>>
    suspend fun getPatientById(patientId: String): ApiResult<Patient>
    suspend fun createPatient(
        name: String, age: Int?, gender: String?, height: Int?, weight: Int?,
        medicalHistory: String?, characteristics: String?
    ): ApiResult<Patient>
    suspend fun updatePatient(
        patientId: String, name: String?, age: Int?, gender: String?,
        height: Int?, weight: Int?, medicalHistory: String?, characteristics: String?
    ): ApiResult<Patient>
    suspend fun updateFence(patientId: String, fence: GeoFence): ApiResult<Unit>
    suspend fun getGuardians(patientId: String): ApiResult<List<Guardian>>
    suspend fun inviteGuardian(patientId: String, request: InviteGuardianRequest): ApiResult<Unit>
    suspend fun removeGuardian(patientId: String, guardianId: String): ApiResult<Unit>
    suspend fun transferOwner(patientId: String, toUserId: String): ApiResult<Unit>
    suspend fun acceptGuardianInvite(inviteCode: String): ApiResult<Unit>
    suspend fun rejectGuardianInvite(inviteCode: String): ApiResult<Unit>
}
