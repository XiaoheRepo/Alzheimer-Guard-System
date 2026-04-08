package com.xiaohelab.guard.android.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.domain.model.TaskEvent
import com.xiaohelab.guard.android.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val loading: Boolean = false,
    val task: Task? = null,
    val events: List<TaskEvent> = emptyList(),
    val error: String? = null
)

sealed interface TaskDetailUiEffect {
    data class NavigateToClose(val taskId: String) : TaskDetailUiEffect
    data class NavigateToTrack(val taskId: String) : TaskDetailUiEffect
    object NavigateBack : TaskDetailUiEffect
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _state = MutableStateFlow(TaskDetailUiState())
    val state: StateFlow<TaskDetailUiState> = _state

    private val _effect = MutableSharedFlow<TaskDetailUiEffect>()
    val effect: SharedFlow<TaskDetailUiEffect> = _effect

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = taskRepository.fetchTaskById(taskId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, task = result.data) }
                    loadEvents()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    private suspend fun loadEvents() {
        when (val result = taskRepository.pollEvents(taskId, null)) {
            is ApiResult.Success -> _state.update { it.copy(events = result.data) }
            is ApiResult.Failure -> { /* non-fatal */ }
        }
    }

    fun closeTask() {
        viewModelScope.launch { _effect.emit(TaskDetailUiEffect.NavigateToClose(taskId)) }
    }

    fun trackTask() {
        viewModelScope.launch { _effect.emit(TaskDetailUiEffect.NavigateToTrack(taskId)) }
    }
}
