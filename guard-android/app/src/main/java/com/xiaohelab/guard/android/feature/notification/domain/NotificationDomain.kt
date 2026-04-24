package com.xiaohelab.guard.android.feature.notification.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.notification.data.NotificationApi
import com.xiaohelab.guard.android.feature.notification.data.NotificationDto
import com.xiaohelab.guard.android.feature.notification.data.NotificationListDto
import javax.inject.Inject

interface NotificationRepository {
    suspend fun listNotifications(): MhResult<NotificationListDto>
    suspend fun markRead(notificationId: String): MhResult<Unit>
    suspend fun markAllRead(): MhResult<Unit>
}

class NotificationRepositoryImpl @Inject constructor(private val api: NotificationApi) : NotificationRepository {
    override suspend fun listNotifications() = handleEnvelope { api.listNotifications() }
    override suspend fun markRead(notificationId: String) =
        when (val r = handleEnvelope { api.markRead(notificationId) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
    override suspend fun markAllRead() =
        when (val r = handleEnvelope { api.markAllRead() }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
}

// --- UseCases ---
class ListNotificationsUseCase @Inject constructor(private val repo: NotificationRepository) {
    suspend operator fun invoke() = repo.listNotifications()
}

class MarkNotificationReadUseCase @Inject constructor(private val repo: NotificationRepository) {
    suspend operator fun invoke(notificationId: String) = repo.markRead(notificationId)
}

class MarkAllNotificationsReadUseCase @Inject constructor(private val repo: NotificationRepository) {
    suspend operator fun invoke() = repo.markAllRead()
}
