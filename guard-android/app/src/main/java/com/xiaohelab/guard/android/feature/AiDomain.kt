package com.xiaohelab.guard.android.feature.ai.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.ai.data.AiSessionApi
import com.xiaohelab.guard.android.feature.ai.data.AiSessionDto
import com.xiaohelab.guard.android.feature.ai.data.CreateSessionRequest
import com.xiaohelab.guard.android.feature.ai.data.IntentDto
import javax.inject.Inject

interface AiRepository {
    suspend fun createSession(patientId: String?): MhResult<AiSessionDto>
    suspend fun getSession(sessionId: String): MhResult<AiSessionDto>
    suspend fun getIntent(intentId: String): MhResult<IntentDto>
    suspend fun confirmIntent(intentId: String): MhResult<Unit>
    suspend fun cancelIntent(intentId: String): MhResult<Unit>
}

class AiRepositoryImpl @Inject constructor(private val api: AiSessionApi) : AiRepository {
    override suspend fun createSession(patientId: String?) =
        handleEnvelope { api.createSession(CreateSessionRequest(patientId)) }

    override suspend fun getSession(sessionId: String) = handleEnvelope { api.getSession(sessionId) }
    override suspend fun getIntent(intentId: String) = handleEnvelope { api.getIntent(intentId) }

    override suspend fun confirmIntent(intentId: String) =
        when (val r = handleEnvelope { api.confirmIntent(intentId) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }

    override suspend fun cancelIntent(intentId: String) =
        when (val r = handleEnvelope { api.cancelIntent(intentId) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
}

// --- UseCases ---
class CreateAiSessionUseCase @Inject constructor(private val repo: AiRepository) {
    suspend operator fun invoke(patientId: String?) = repo.createSession(patientId)
}

class GetAiSessionUseCase @Inject constructor(private val repo: AiRepository) {
    suspend operator fun invoke(sessionId: String) = repo.getSession(sessionId)
}

class GetIntentUseCase @Inject constructor(private val repo: AiRepository) {
    suspend operator fun invoke(intentId: String) = repo.getIntent(intentId)
}

class ConfirmIntentUseCase @Inject constructor(private val repo: AiRepository) {
    suspend operator fun invoke(intentId: String) = repo.confirmIntent(intentId)
}

class CancelIntentUseCase @Inject constructor(private val repo: AiRepository) {
    suspend operator fun invoke(intentId: String) = repo.cancelIntent(intentId)
}
