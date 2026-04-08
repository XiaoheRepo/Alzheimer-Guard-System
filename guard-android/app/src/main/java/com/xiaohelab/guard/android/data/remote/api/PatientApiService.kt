package com.xiaohelab.guard.android.data.remote.api

import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.data.remote.dto.CreatePatientRequestDto
import com.xiaohelab.guard.android.data.remote.dto.GuardianDto
import com.xiaohelab.guard.android.data.remote.dto.GuardianInviteActionDto
import com.xiaohelab.guard.android.data.remote.dto.InviteGuardianRequestDto
import com.xiaohelab.guard.android.data.remote.dto.PatientDto
import com.xiaohelab.guard.android.data.remote.dto.TransferOwnerRequestDto
import com.xiaohelab.guard.android.data.remote.dto.UpdateFenceRequestDto
import com.xiaohelab.guard.android.data.remote.dto.UpdatePatientRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PatientApiService {
    @GET("api/v1/patients")
    suspend fun getMyPatients(): Response<ApiResponseDto<List<PatientDto>>>

    @POST("api/v1/patients")
    suspend fun createPatient(@Body request: CreatePatientRequestDto): Response<ApiResponseDto<PatientDto>>

    @GET("api/v1/patients/{id}")
    suspend fun getPatientById(@Path("id") id: String): Response<ApiResponseDto<PatientDto>>

    @PATCH("api/v1/patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: String,
        @Body request: UpdatePatientRequestDto
    ): Response<ApiResponseDto<PatientDto>>

    @PUT("api/v1/patients/{id}/fence")
    suspend fun updateFence(
        @Path("id") id: String,
        @Body request: UpdateFenceRequestDto
    ): Response<ApiResponseDto<Unit>>

    @GET("api/v1/patients/{id}/guardians")
    suspend fun getGuardians(@Path("id") id: String): Response<ApiResponseDto<List<GuardianDto>>>

    @POST("api/v1/patients/{id}/guardians/invite")
    suspend fun inviteGuardian(
        @Path("id") id: String,
        @Body request: InviteGuardianRequestDto
    ): Response<ApiResponseDto<Unit>>

    @DELETE("api/v1/patients/{id}/guardians/{guardianId}")
    suspend fun removeGuardian(
        @Path("id") id: String,
        @Path("guardianId") guardianId: String
    ): Response<ApiResponseDto<Unit>>

    @POST("api/v1/patients/{id}/guardians/transfer")
    suspend fun transferOwner(
        @Path("id") id: String,
        @Body request: TransferOwnerRequestDto
    ): Response<ApiResponseDto<Unit>>

    @POST("api/v1/guardians/invites/accept")
    suspend fun acceptInvite(@Body request: GuardianInviteActionDto): Response<ApiResponseDto<Unit>>

    @POST("api/v1/guardians/invites/reject")
    suspend fun rejectInvite(@Body request: GuardianInviteActionDto): Response<ApiResponseDto<Unit>>
}
