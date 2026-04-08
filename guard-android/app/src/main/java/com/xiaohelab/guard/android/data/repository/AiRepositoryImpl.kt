package com.xiaohelab.guard.android.data.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.network.NetworkModule.safeApiCall
import com.xiaohelab.guard.android.data.mapper.toDomain
import com.xiaohelab.guard.android.data.remote.api.AiApiService
import com.xiaohelab.guard.android.data.remote.dto.CreateMemoryNoteRequestDto
import com.xiaohelab.guard.android.data.remote.dto.CreateSessionRequestDto
import com.xiaohelab.guard.android.data.remote.dto.SendMessageRequestDto
import com.xiaohelab.guard.android.domain.model.AiMemoryNote
import com.xiaohelab.guard.android.domain.model.AiMessage
import com.xiaohelab.guard.android.domain.model.AiQuota
import com.xiaohelab.guard.android.domain.model.AiSession
import com.xiaohelab.guard.android.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val api: AiApiService
) : AiRepository {

    override suspend fun getSessions(patientId: String): ApiResult<List<AiSession>> =
        safeApiCall { api.getSessions(patientId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun createSession(patientId: String, title: String?): ApiResult<AiSession> =
        safeApiCall { api.createSession(CreateSessionRequestDto(patientId, title)) }
            .map { it.toDomain() }

    override suspend fun getMessages(sessionId: String): ApiResult<List<AiMessage>> =
        safeApiCall { api.getMessages(sessionId) }.map { list -> list.map { it.toDomain() } }

    /** SSE 流式 — 逐行 emit 文本增量（HandBook §9.2） */
    override fun sendMessageWithStream(sessionId: String, content: String): Flow<String> = flow {
        val response = api.sendMessageStream(sessionId, SendMessageRequestDto(content))
        if (!response.isSuccessful) {
            emit("[ERROR:${response.code()}]")
            return@flow
        }
        val body = response.body() ?: return@flow
        body.source().use { source ->
            val buffer = okio.Buffer()
            while (!source.exhausted()) {
                source.read(buffer, 8192)
                val line = buffer.readUtf8()
                // SSE: "data: <chunk>\n\n"
                line.split("\n")
                    .filter { it.startsWith("data:") }
                    .forEach { emit(it.removePrefix("data:").trim()) }
            }
        }
    }

    override suspend fun getQuota(): ApiResult<AiQuota> =
        safeApiCall { api.getQuota() }.map { it.toDomain() }

    override suspend fun getMemoryNotes(patientId: String): ApiResult<List<AiMemoryNote>> =
        safeApiCall { api.getMemoryNotes(patientId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun createMemoryNote(patientId: String, content: String): ApiResult<AiMemoryNote> =
        safeApiCall { api.createMemoryNote(CreateMemoryNoteRequestDto(patientId, content)) }
            .map { it.toDomain() }

    override suspend fun deleteMemoryNote(noteId: String): ApiResult<Unit> =
        safeApiCall { api.deleteMemoryNote(noteId) }
}
