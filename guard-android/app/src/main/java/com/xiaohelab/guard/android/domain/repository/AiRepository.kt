package com.xiaohelab.guard.android.domain.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.AiMemoryNote
import com.xiaohelab.guard.android.domain.model.AiMessage
import com.xiaohelab.guard.android.domain.model.AiQuota
import com.xiaohelab.guard.android.domain.model.AiSession
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun getSessions(patientId: String): ApiResult<List<AiSession>>
    suspend fun createSession(patientId: String, title: String?): ApiResult<AiSession>
    suspend fun getMessages(sessionId: String): ApiResult<List<AiMessage>>
    fun sendMessageWithStream(sessionId: String, content: String): Flow<String>
    suspend fun getQuota(): ApiResult<AiQuota>
    suspend fun getMemoryNotes(patientId: String): ApiResult<List<AiMemoryNote>>
    suspend fun createMemoryNote(patientId: String, content: String): ApiResult<AiMemoryNote>
    suspend fun deleteMemoryNote(noteId: String): ApiResult<Unit>
}
