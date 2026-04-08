package com.xiaohelab.guard.android.feature.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.AiMessage
import com.xiaohelab.guard.android.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val streamingContent: String = "",
    val isStreaming: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface AiChatUiEffect {
    data class ShowToast(val message: String) : AiChatUiEffect
}

@HiltViewModel
class AiChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state

    private val _effect = MutableSharedFlow<AiChatUiEffect>()
    val effect: SharedFlow<AiChatUiEffect> = _effect

    init { loadMessages() }

    fun loadMessages() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = aiRepository.getMessages(sessionId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, messages = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onInputChange(v: String) = _state.update { it.copy(inputText = v) }

    fun sendMessage() {
        val content = _state.value.inputText.trim()
        if (content.isBlank() || _state.value.isStreaming) return
        _state.update { it.copy(inputText = "", isStreaming = true, streamingContent = "", error = null) }
        viewModelScope.launch {
            aiRepository.sendMessageWithStream(sessionId, content)
                .catch { e ->
                    _state.update { it.copy(isStreaming = false, error = e.message ?: "发送失败") }
                }
                .onCompletion {
                    // When stream ends, reload messages to get the final persisted record
                    loadMessages()
                    _state.update { it.copy(isStreaming = false, streamingContent = "") }
                }
                .collect { chunk ->
                    _state.update { it.copy(streamingContent = it.streamingContent + chunk) }
                }
        }
    }
}
