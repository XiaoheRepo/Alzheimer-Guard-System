package com.xiaohelab.guard.android.feature.notification.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.eventbus.AppEvent
import com.xiaohelab.guard.android.core.eventbus.AppEventBus
import com.xiaohelab.guard.android.feature.notification.domain.ListNotificationsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * M6 通知补洞 Worker。
 * HC-05: 轮询间隔来自 RemoteConfigRepository.getInt(KEY_NOTIFICATION_POLL_INTERVAL_SEC)，禁止硬编码。
 * 每次轮询拉取通知列表，通过 AppEventBus 广播未读数变化（HC-01 六域隔离）。
 * 离线写队列使用入队时固化的 X-Request-Id（由 RequestIdInterceptor 处理）。
 */
@HiltWorker
class NotificationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val listNotifications: ListNotificationsUseCase,
    private val eventBus: AppEventBus,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            when (val r = listNotifications()) {
                is MhResult.Success -> {
                    val count = r.data.unreadCount
                    eventBus.tryEmit(AppEvent.NotificationUnreadCountChanged(count))
                    Timber.d("NotificationSyncWorker: unread=$count")
                    Result.success()
                }
                is MhResult.Failure -> {
                    Timber.w("NotificationSyncWorker failed: ${r.error.code}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "NotificationSyncWorker exception")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "notification_sync_periodic"

        /**
         * HC-05: 轮询间隔从 RemoteConfig 获取（KEY_NOTIFICATION_POLL_INTERVAL_SEC），兜底 60s。
         * 注意: WorkManager PeriodicWorkRequest 最小间隔为 15 分钟；
         * 实际间隔以 remoteConfig 值为准，低于 15 分钟时 WorkManager 会自动 clamp。
         */
        fun schedule(context: Context, intervalSeconds: Long) {
            val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                repeatInterval = intervalSeconds,
                repeatIntervalTimeUnit = TimeUnit.SECONDS,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
