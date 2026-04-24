package com.xiaohelab.guard.android.feature.clue.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.clue.data.ClueApi
import com.xiaohelab.guard.android.feature.clue.data.ClueDto
import com.xiaohelab.guard.android.feature.clue.data.ClueListDto
import com.xiaohelab.guard.android.feature.clue.data.CreateClueRequest
import com.xiaohelab.guard.android.feature.task.data.LocationDto
import javax.inject.Inject

interface ClueRepository {
    suspend fun listClues(taskId: String): MhResult<ClueListDto>
    suspend fun createClue(taskId: String, type: String, content: String, location: LocationDto?): MhResult<ClueDto>
    suspend fun getClue(taskId: String, clueId: String): MhResult<ClueDto>
}

class ClueRepositoryImpl @Inject constructor(private val api: ClueApi) : ClueRepository {
    override suspend fun listClues(taskId: String) = handleEnvelope { api.listClues(taskId) }
    override suspend fun createClue(taskId: String, type: String, content: String, location: LocationDto?) =
        handleEnvelope { api.createClue(taskId, CreateClueRequest(type, content, location)) }
    override suspend fun getClue(taskId: String, clueId: String) = handleEnvelope { api.getClue(taskId, clueId) }
}

// --- UseCases ---
class ListCluesUseCase @Inject constructor(private val repo: ClueRepository) {
    suspend operator fun invoke(taskId: String) = repo.listClues(taskId)
}

class CreateClueUseCase @Inject constructor(private val repo: ClueRepository) {
    suspend operator fun invoke(taskId: String, type: String, content: String, location: LocationDto?) =
        repo.createClue(taskId, type, content, location)
}

class GetClueUseCase @Inject constructor(private val repo: ClueRepository) {
    suspend operator fun invoke(taskId: String, clueId: String) = repo.getClue(taskId, clueId)
}
