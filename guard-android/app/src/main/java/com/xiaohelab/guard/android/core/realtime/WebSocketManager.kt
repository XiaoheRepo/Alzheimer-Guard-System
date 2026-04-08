package com.xiaohelab.guard.android.core.realtime

import com.xiaohelab.guard.android.BuildConfig
import com.xiaohelab.guard.android.core.database.dao.WsEventDedupDao
import com.xiaohelab.guard.android.core.database.entity.WsEventDedupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Serializable
data class WsEvent(
    val event_id: String,
    val type: String,
    val aggregate_id: String? = null,
    val version: Long = 0,
    val event_time: String? = null,
    val payload: kotlinx.serialization.json.JsonElement? = null
)

/** WebSocket 消费动作（HandBook §11.5） */
sealed class ConsumeAction {
    object Ignore : ConsumeAction()
    object Apply : ConsumeAction()
    data class FetchSnapshot(val aggregateId: String) : ConsumeAction()
}

/**
 * WebSocket 管理器（HandBook §11）
 * - 建链：先获取票据（POST /api/v1/ws/tickets），再连接
 * - 去重：基于 event_id（WsEventDedupDao）
 * - 版本守卫：仅接受 version > localVersion 的事件
 * - 断线重连：指数退避（max 30s）
 * - 4401 关闭码：先刷新会话再重连
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val dedupDao: WsEventDedupDao
) {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 30_000L
        private const val AUTH_CLOSE_CODE = 4401
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private var wsTicket: String? = null
    private var reconnectAttempt = 0
    private var localVersionMap = mutableMapOf<String, Long>()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    private val _connectionState = MutableSharedFlow<WsConnectionState>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val connectionState: SharedFlow<WsConnectionState> = _connectionState

    fun connect(ticket: String) {
        wsTicket = ticket
        reconnectAttempt = 0
        doConnect(ticket)
    }

    private fun doConnect(ticket: String) {
        val url = "${BuildConfig.WS_BASE_URL}api/v1/ws/notifications?ticket=$ticket"
        val request = Request.Builder().url(url).build()
        Timber.tag(TAG).d("Connecting to $url")
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "user_logout")
        webSocket = null
        wsTicket = null
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Timber.tag(TAG).d("WebSocket connected")
            reconnectAttempt = 0
            scope.launch { _connectionState.emit(WsConnectionState.Connected) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch { handleMessage(text) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Timber.tag(TAG).w("WebSocket closing: code=$code reason=$reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Timber.tag(TAG).w("WebSocket closed: code=$code")
            scope.launch {
                _connectionState.emit(WsConnectionState.Disconnected)
                if (code == AUTH_CLOSE_CODE) {
                    // 鉴权类关闭码：先通知上层刷新 Token，再重连
                    _connectionState.emit(WsConnectionState.AuthError)
                } else {
                    scheduleReconnect()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.tag(TAG).e(t, "WebSocket failure")
            scope.launch {
                _connectionState.emit(WsConnectionState.Disconnected)
                scheduleReconnect()
            }
        }
    }

    private suspend fun handleMessage(text: String) {
        try {
            val event = Json { ignoreUnknownKeys = true }.decodeFromString<WsEvent>(text)
            val action = evaluate(event)
            when (action) {
                is ConsumeAction.Ignore -> {
                    Timber.tag(TAG).d("Ignoring event ${event.event_id} (dedup or stale version)")
                }
                is ConsumeAction.Apply -> {
                    dedupDao.insert(WsEventDedupEntity(event.event_id))
                    event.aggregate_id?.let { localVersionMap[it] = event.version }
                    _events.emit(event)
                }
                is ConsumeAction.FetchSnapshot -> {
                    // 触发快照拉取（由上层 Repository 监听并处理）
                    _events.emit(event.copy(type = "FETCH_SNAPSHOT_REQUESTED"))
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse WebSocket message: $text")
        }
    }

    private suspend fun evaluate(event: WsEvent): ConsumeAction {
        if (dedupDao.contains(event.event_id)) return ConsumeAction.Ignore
        val localVersion = event.aggregate_id?.let { localVersionMap[it] } ?: -1L
        return when {
            event.version <= localVersion -> ConsumeAction.Ignore
            event.version > localVersion + 1 -> ConsumeAction.FetchSnapshot(event.aggregate_id ?: "")
            else -> ConsumeAction.Apply
        }
    }

    private suspend fun scheduleReconnect() {
        val ticket = wsTicket ?: return
        val delay = min(BASE_DELAY_MS * (1L shl reconnectAttempt), MAX_DELAY_MS)
        Timber.tag(TAG).d("Reconnecting in ${delay}ms (attempt ${reconnectAttempt + 1})")
        _connectionState.emit(WsConnectionState.Reconnecting(delay))
        delay(delay)
        reconnectAttempt++
        doConnect(ticket)
    }
}

sealed class WsConnectionState {
    object Connected : WsConnectionState()
    object Disconnected : WsConnectionState()
    object AuthError : WsConnectionState()
    data class Reconnecting(val delayMs: Long) : WsConnectionState()
}
