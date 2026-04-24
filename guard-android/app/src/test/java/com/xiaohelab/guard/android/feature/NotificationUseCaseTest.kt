package com.xiaohelab.guard.android.feature.notification

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.feature.notification.data.NotificationDto
import com.xiaohelab.guard.android.feature.notification.data.NotificationListDto
import com.xiaohelab.guard.android.feature.notification.domain.ListNotificationsUseCase
import com.xiaohelab.guard.android.feature.notification.domain.MarkAllNotificationsReadUseCase
import com.xiaohelab.guard.android.feature.notification.domain.MarkNotificationReadUseCase
import com.xiaohelab.guard.android.feature.notification.domain.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Notification domain UseCases.
 * HC-ID-String: notification_id 为 String。
 */
class NotificationUseCaseTest {

    private val repo: NotificationRepository = mockk()
    private val listNotifications = ListNotificationsUseCase(repo)
    private val markRead = MarkNotificationReadUseCase(repo)
    private val markAllRead = MarkAllNotificationsReadUseCase(repo)

    private val sampleNotif = NotificationDto(
        notificationId = "notif_001",  // HC-ID-String: String
        type = "TASK_UPDATE",
        title = "任务状态更新",
        body = "寻回任务已取消",
        read = false,
        createdAt = "2024-01-01T00:00:00Z",
        deepLink = null,
    )

    @Test
    fun `listNotifications returns list with unread count`() = runTest {
        val expected = NotificationListDto(
            items = listOf(sampleNotif),
            unreadCount = 1,
            total = 1,
        )
        coEvery { repo.listNotifications() } returns MhResult.Success(expected, null)

        val result = listNotifications()

        assertTrue(result is MhResult.Success)
        val data = (result as MhResult.Success).data
        assertEquals(1, data.unreadCount)
        assertEquals("notif_001", data.items[0].notificationId)
        coVerify { repo.listNotifications() }
    }

    @Test
    fun `markRead delegates notification_id to repository`() = runTest {
        coEvery { repo.markRead("notif_001") } returns MhResult.Success(Unit, null)

        val result = markRead("notif_001")

        assertTrue(result is MhResult.Success)
        coVerify { repo.markRead("notif_001") }
    }

    @Test
    fun `markAllRead delegates to repository`() = runTest {
        coEvery { repo.markAllRead() } returns MhResult.Success(Unit, null)

        val result = markAllRead()

        assertTrue(result is MhResult.Success)
        coVerify { repo.markAllRead() }
    }

    @Test
    fun `markRead propagates E_NOTIF_4041 failure`() = runTest {
        val error = com.xiaohelab.guard.android.core.common.DomainException("E_NOTIF_4041", "Notification not found")
        coEvery { repo.markRead("notif_999") } returns MhResult.Failure(error)

        val result = markRead("notif_999")

        assertTrue(result is MhResult.Failure)
        assertEquals("E_NOTIF_4041", (result as MhResult.Failure).error.code)
    }
}
