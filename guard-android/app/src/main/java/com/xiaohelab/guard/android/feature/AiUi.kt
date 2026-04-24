@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.ai.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.feature.ai.data.AiMessageChunk
import com.xiaohelab.guard.android.feature.ai.data.AiMessageRequest
import com.xiaohelab.guard.android.feature.ai.data.IntentDto
import com.xiaohelab.guard.android.feature.ai.domain.AiRepository
import com.xiaohelab.guard.android.feature.ai.domain.CancelIntentUseCase
import com.xiaohelab.guard.android.feature.ai.domain.ConfirmIntentUseCase
import com.xiaohelab.guard.android.feature.ai.domain.CreateAiSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Named

// ─── Chat bubble model ────────────────────────────────────────────────────────

data class ChatMessage(
    val role: String,        // "user" or "assistant"
    val text: String,
    val streaming: Boolean = false,
)

// ─── AiChatViewModel ──────────────────────────────────────────────────────────

data class AiChatUiState(
    val initializing: Boolean = true,
    val sessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val streaming: Boolean = false,
    val error: DomainException? = null,
    val pendingIntent: IntentDto? = null,
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val createSession: CreateAiSessionUseCase,
    private val confirmIntent: ConfirmIntentUseCase,
    private val cancelIntent: CancelIntentUseCase,
    private val aiRepo: AiRepository,
    /**
     * 手册 §11: AI SSE 使用 OkHttp EventSource。
     * 使用带认证拦截器的共享 OkHttpClient（NetworkModule 提供）。
     */
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    @Named("api_base_url") private val apiBaseUrl: String,
) : ViewModel() {
    private val _s = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _s.asStateFlow()

    fun init(existingSessionId: String?, patientId: String?) {
        if (existingSessionId != null) {
            _s.update { it.copy(initializing = false, sessionId = existingSessionId) }
            return
        }
        viewModelScope.launch {
            when (val r = createSession(patientId)) {
                is MhResult.Success -> _s.update { it.copy(initializing = false, sessionId = r.data.sessionId) }
                is MhResult.Failure -> _s.update { it.copy(initializing = false, error = r.error) }
            }
        }
    }

    fun onInputChange(v: String) = _s.update { it.copy(inputText = v) }

    fun sendMessage() {
        val s = _s.value
        val sessionId = s.sessionId ?: return
        val content = s.inputText.trim().ifEmpty { return }
        _s.update {
            it.copy(
                inputText = "",
                streaming = true,
                messages = it.messages + ChatMessage("user", content) + ChatMessage("assistant", "", streaming = true),
            )
        }
        viewModelScope.launch {
            sseStream(sessionId, content).collect { chunk ->
                if (chunk.done) {
                    _s.update { state ->
                        val msgs = state.messages.toMutableList()
                        val lastIdx = msgs.indexOfLast { it.role == "assistant" }
                        if (lastIdx >= 0) msgs[lastIdx] = msgs[lastIdx].copy(streaming = false)
                        state.copy(streaming = false, messages = msgs)
                    }
                    // intent 确认
                    chunk.intentId?.let { intentId ->
                        when (val r = aiRepo.getIntent(intentId)) {
                            is MhResult.Success -> _s.update { it.copy(pendingIntent = r.data) }
                            else -> Unit
                        }
                    }
                } else {
                    chunk.delta?.let { delta ->
                        _s.update { state ->
                            val msgs = state.messages.toMutableList()
                            val lastIdx = msgs.indexOfLast { it.role == "assistant" }
                            if (lastIdx >= 0) {
                                msgs[lastIdx] = msgs[lastIdx].copy(text = msgs[lastIdx].text + delta)
                            }
                            state.copy(messages = msgs)
                        }
                    }
                }
            }
        }
    }

    fun confirmPendingIntent() {
        val intentId = _s.value.pendingIntent?.intentId ?: return
        _s.update { it.copy(pendingIntent = null) }
        viewModelScope.launch { confirmIntent(intentId) }
    }

    fun cancelPendingIntent() {
        val intentId = _s.value.pendingIntent?.intentId ?: return
        _s.update { it.copy(pendingIntent = null) }
        viewModelScope.launch { cancelIntent(intentId) }
    }

    /**
     * 手册 §11 / HC-Realtime: AI 流式 SSE 使用 okhttp-sse EventSource 桥接为 Flow<AiMessageChunk>。
     * 断线退避由 EventSource 框架层处理（2^n + jitter，上限 30s）。
     */
    private fun sseStream(sessionId: String, content: String): Flow<AiMessageChunk> = callbackFlow {
        val body = json.encodeToString(AiMessageRequest.serializer(), AiMessageRequest(content))
        val request = Request.Builder()
            .url("${apiBaseUrl.trimEnd('/')}/api/v1/ai/sessions/$sessionId/messages")
            .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), body))
            .build()
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                runCatching {
                    val chunk = json.decodeFromString(AiMessageChunk.serializer(), data)
                    trySend(chunk)
                }
            }
            override fun onClosed(eventSource: EventSource) { close() }
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) { close(t) }
        }
        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }
}

// ─── AiIntentConfirmDialog ────────────────────────────────────────────────────

/** AI Intent 确认对话框。 */
@Composable
fun AiIntentConfirmDialog(
    intent: IntentDto,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ai_intent_confirm_title)) },
        text = {
            Text(
                stringResource(R.string.ai_intent_confirm_body, intent.description)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ai_intent_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.ai_intent_cancel)) }
        },
    )
}

// ─── AiChatScreen (MH-AI-00) ─────────────────────────────────────────────────

/**
 * MH-AI-00: AI 双模交互聊天界面。
 * 流式 SSE 渐现文字；intent 触发确认对话框。
 * HC-05 额度来自远端配置（E_AI_4291 = 超额）。
 */
@Composable
fun AiChatScreen(
    sessionId: String?,
    patientId: String? = null,
    onBack: () -> Unit,
    vm: AiChatViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) { vm.init(sessionId, patientId) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    // Intent 确认对话框
    state.pendingIntent?.let { intent ->
        AiIntentConfirmDialog(
            intent = intent,
            onConfirm = vm::confirmPendingIntent,
            onCancel = vm::cancelPendingIntent,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_chat_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize()) {
            when {
                state.initializing -> MhLoading(Modifier.weight(1f))
                else -> {
                    // 消息列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.messages) { msg ->
                            ChatBubble(msg)
                        }
                    }
                    // 输入区
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.inputText,
                            onValueChange = vm::onInputChange,
                            placeholder = { Text(stringResource(R.string.ai_input_hint)) },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                        )
                        IconButton(
                            onClick = vm::sendMessage,
                            enabled = !state.streaming && state.inputText.isNotBlank(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.common_submit))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            AnimatedContent(targetState = msg.text) { text ->
                Text(
                    text = if (msg.streaming && text.isEmpty()) "…" else text,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
