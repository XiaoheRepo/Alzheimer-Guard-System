package com.xiaohelab.guard.android.data.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.database.dao.NotificationDao
import com.xiaohelab.guard.android.core.database.entity.NotificationEntity
import com.xiaohelab.guard.android.core.network.NetworkModule.safeApiCall
import com.xiaohelab.guard.android.data.mapper.toDomain
import com.xiaohelab.guard.android.data.remote.api.NotificationApiService
import com.xiaohelab.guard.android.domain.model.Notification
import com.xiaohelab.guard.android.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApiService,
    private val dao: NotificationDao
) : NotificationRepository {

    override fun observeNotifications(): Flow<List<Notification>> =
        dao.observeAll().map { list ->
            list.map { e ->
                Notification(
                    id = e.notificationId,
                    type = e.type,
                    title = e.title,
                    content = e.content,
                    isRead = e.readStatus,
                    createdAt = e.createdAt,
                    relatedId = e.relatedId,
                    relatedType = e.relatedType
                )
            }
        }

    override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    override suspend fun fetchNotifications(
        cursor: String?,
        limit: Int
    ): ApiResult<Pair<List<Notification>, String?>> {
        val result = safeApiCall { api.getNotifications(cursor, limit) }
        return if (result is ApiResult.Success) {
            val page = result.data
            val entities = page.items.map { dto ->
                NotificationEntity(
                    notificationId = dto.id,
                    type = dto.type,
                    title = dto.title,
                    content = dto.content,
                    readStatus = dto.isRead,
                    createdAt = dto.createdAt,
                    relatedId = dto.relatedId,
                    relatedType = dto.relatedType
                )
            }
            dao.upsertAll(entities)
            ApiResult.Success(Pair(page.items.map { it.toDomain() }, page.nextCursor))
        } else {
            ApiResult.Failure(
                code = (result as ApiResult.Failure).code,
                message = result.message,
                traceId = result.traceId
            )
        }
    }

    override suspend fun markRead(id: String): ApiResult<Unit> {
        dao.markRead(id)
        return safeApiCall { api.markRead(id) }
    }

    override suspend fun markAllRead(): ApiResult<Unit> {
        dao.markAllRead()
        return safeApiCall { api.markAllRead() }
    }
}
