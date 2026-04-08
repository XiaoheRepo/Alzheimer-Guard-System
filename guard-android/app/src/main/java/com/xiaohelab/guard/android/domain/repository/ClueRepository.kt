package com.xiaohelab.guard.android.domain.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.Clue
import com.xiaohelab.guard.android.domain.model.ResourceInfo

interface ClueRepository {
    suspend fun resolveResourceToken(token: String): ApiResult<ResourceInfo>
    suspend fun submitManualClue(
        anonymousToken: String,
        name: String?,
        phone: String?,
        description: String,
        locationDesc: String?,
        lat: Double?,
        lng: Double?,
        images: List<String>
    ): ApiResult<Clue>
    suspend fun reportClue(
        taskId: String,
        description: String,
        locationDesc: String?,
        lat: Double?,
        lng: Double?,
        contactPhone: String?,
        images: List<String>
    ): ApiResult<Clue>
    suspend fun getClueById(clueId: String): ApiResult<Clue>
    suspend fun getAnonymousToken(): String?
    suspend fun saveAnonymousToken(token: String)
    suspend fun clearAnonymousToken()
}
