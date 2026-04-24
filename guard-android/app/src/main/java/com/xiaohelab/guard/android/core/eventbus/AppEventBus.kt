package com.xiaohelab.guard.android.core.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-feature event bus (HC-01 六域隔离的合法通路)。
 * feature 模块只允许通过 [AppEvent] 的 seal 枝叶跨域，不得直接 import 对方内部类。
 */
sealed interface AppEvent {
    data object SessionInvalidated : AppEvent
    data class StateChanged(
        val type: String,
        val aggregateId: String,
        val version: Long,
        val eventTime: String,
        val traceId: String?,
        val payloadJson: String,
    ) : AppEvent

    data class RemoteConfigRefreshed(val updatedAt: String) : AppEvent

    /** M6: 通知中心未读计数变化（WorkManager 轮询或 WS 推送后广播）。 */
    data class NotificationUnreadCountChanged(val count: Int) : AppEvent
}

@Singleton
class AppEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    suspend fun emit(event: AppEvent) = _events.emit(event)
    fun tryEmit(event: AppEvent): Boolean = _events.tryEmit(event)
}
