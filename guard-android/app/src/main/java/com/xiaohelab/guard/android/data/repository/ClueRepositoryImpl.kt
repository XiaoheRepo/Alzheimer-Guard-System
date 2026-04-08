package com.xiaohelab.guard.android.data.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.TokenManager
import com.xiaohelab.guard.android.core.network.NetworkModule.safeApiCall
import com.xiaohelab.guard.android.data.mapper.toDomain
import com.xiaohelab.guard.android.data.remote.api.ClueApiService
import com.xiaohelab.guard.android.data.remote.dto.ManualClueRequestDto
import com.xiaohelab.guard.android.data.remote.dto.ReportClueRequestDto
import com.xiaohelab.guard.android.domain.model.Clue
import com.xiaohelab.guard.android.domain.model.ResourceInfo
import com.xiaohelab.guard.android.domain.repository.ClueRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClueRepositoryImpl @Inject constructor(
    private val api: ClueApiService,
    private val tokenManager: TokenManager
) : ClueRepository {

    override suspend fun resolveResourceToken(token: String): ApiResult<ResourceInfo> =
        safeApiCall { api.resolveResourceToken(token) }.map { it.toDomain() }

    override suspend fun submitManualClue(
        anonymousToken: String, name: String?, phone: String?, description: String,
        locationDesc: String?, lat: Double?, lng: Double?, images: List<String>
    ): ApiResult<Clue> = safeApiCall {
        api.submitManualClue(
            ManualClueRequestDto(anonymousToken, name, phone, description, locationDesc, lat, lng, images)
        )
    }.map { it.toDomain() }

    override suspend fun reportClue(
        taskId: String, description: String, locationDesc: String?, lat: Double?, lng: Double?,
        contactPhone: String?, images: List<String>
    ): ApiResult<Clue> = safeApiCall {
        api.reportClue(
            ReportClueRequestDto(taskId, description, locationDesc, lat, lng, contactPhone, images)
        )
    }.map { it.toDomain() }

    override suspend fun getClueById(clueId: String): ApiResult<Clue> =
        safeApiCall { api.getClueById(clueId) }.map { it.toDomain() }

    override suspend fun getAnonymousToken(): String? = tokenManager.getAnonymousToken()

    override suspend fun saveAnonymousToken(token: String) = tokenManager.saveAnonymousToken(token)

    override suspend fun clearAnonymousToken() = tokenManager.clearAnonymousToken()
}
