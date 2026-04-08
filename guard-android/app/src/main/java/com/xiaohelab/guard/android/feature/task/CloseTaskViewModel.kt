package com.xiaohelab.guard.android.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.CloseTaskRequest
import com.xiaohelab.guard.android.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloseTaskUiState(
    val reason: CloseTaskRequest.CloseReason = CloseTaskRequest.CloseReason.FOUND,
    val remarks: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface CloseTaskUiEffect {
    object Success : CloseTaskUiEffect
}

@HiltViewModel
class CloseTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _state = MutableStateFlow(CloseTaskUiState())
    val state: StateFlow<CloseTaskUiState> = _state

    private val _effect = MutableSharedFlow<CloseTaskUiEffect>()
    val effect: SharedFlow<CloseTaskUiEffect> = _effect

    fun onReasonChange(r: CloseTaskRequest.CloseReason) = _state.update { it.copy(reason = r) }
    fun onRemarksChange(v: String) = _state.update { it.copy(remarks = v, error = null) }

    fun submit() {
        val s = _state.value
        // 虚假警报需要备注（5-256 字符）
        if (s.reason == CloseTaskRequest.CloseReason.FALSE_ALARM &&
            (s.remarks.trim().length < 5 || s.remarks.trim().length > 256)
        ) {
            _state.update { it.copy(error = "虚假警报说明需在 5-256 字之间") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = taskRepository.closeTask(
                taskId,
                CloseTaskRequest(s.reason, s.remarks.ifBlank { null })
            )) {
                is ApiResult.Success -> _effect.emit(CloseTaskUiEffect.Success)
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
