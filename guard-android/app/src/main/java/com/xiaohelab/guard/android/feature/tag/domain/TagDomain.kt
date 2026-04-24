package com.xiaohelab.guard.android.feature.tag.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.tag.data.TagApi
import com.xiaohelab.guard.android.feature.tag.data.TagBindRequest
import com.xiaohelab.guard.android.feature.tag.data.TagDto
import com.xiaohelab.guard.android.feature.tag.data.TagListDto
import com.xiaohelab.guard.android.feature.tag.data.TagUpdateRequest
import javax.inject.Inject

interface TagRepository {
    suspend fun listTags(patientId: String): MhResult<TagListDto>
    suspend fun bindTag(patientId: String, tagCode: String, deviceType: String, alias: String?): MhResult<TagDto>
    suspend fun updateTag(patientId: String, tagCode: String, alias: String?): MhResult<TagDto>
    suspend fun unbindTag(patientId: String, tagCode: String): MhResult<Unit>
}

class TagRepositoryImpl @Inject constructor(private val api: TagApi) : TagRepository {
    override suspend fun listTags(patientId: String) = handleEnvelope { api.listTags(patientId) }
    override suspend fun bindTag(patientId: String, tagCode: String, deviceType: String, alias: String?) =
        handleEnvelope { api.bindTag(patientId, TagBindRequest(tagCode, deviceType, alias)) }
    override suspend fun updateTag(patientId: String, tagCode: String, alias: String?) =
        handleEnvelope { api.updateTag(patientId, tagCode, TagUpdateRequest(alias)) }
    override suspend fun unbindTag(patientId: String, tagCode: String) =
        when (val r = handleEnvelope { api.unbindTag(patientId, tagCode) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
}

// --- UseCases ---
class ListTagsUseCase @Inject constructor(private val repo: TagRepository) {
    suspend operator fun invoke(patientId: String) = repo.listTags(patientId)
}

class BindTagUseCase @Inject constructor(private val repo: TagRepository) {
    suspend operator fun invoke(patientId: String, tagCode: String, deviceType: String, alias: String?) =
        repo.bindTag(patientId, tagCode, deviceType, alias)
}

class UpdateTagUseCase @Inject constructor(private val repo: TagRepository) {
    suspend operator fun invoke(patientId: String, tagCode: String, alias: String?) =
        repo.updateTag(patientId, tagCode, alias)
}

class UnbindTagUseCase @Inject constructor(private val repo: TagRepository) {
    suspend operator fun invoke(patientId: String, tagCode: String) =
        repo.unbindTag(patientId, tagCode)
}
