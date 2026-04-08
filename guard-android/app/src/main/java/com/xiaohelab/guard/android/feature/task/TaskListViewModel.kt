package com.xiaohelab.guard.android.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.AppSettingsManager
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.domain.model.TaskStatus
import com.xiaohelab.guard.android.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val loading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val filterStatus: TaskStatus? = null,
    val error: String? = null
)

sealed interface TaskListUiEffect {
    data class NavigateToDetail(val taskId: String) : TaskListUiEffect
    object NavigateToCreate : TaskListUiEffect
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsManager: AppSettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(TaskListUiState())
    val state: StateFlow<TaskListUiState> = _state

    private val _effect = MutableSharedFlow<TaskListUiEffect>()
    val effect: SharedFlow<TaskListUiEffect> = _effect

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val patientId = settingsManager.currentPatientId.first()
            when (val result = taskRepository.fetchTasks(patientId)) {
                is ApiResult.Success -> _state.update { s ->
                    s.copy(loading = false, tasks = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun setFilter(status: TaskStatus?) = _state.update { it.copy(filterStatus = status) }

    fun openDetail(taskId: String) {
        viewModelScope.launch { _effect.emit(TaskListUiEffect.NavigateToDetail(taskId)) }
    }

    fun openCreate() {
        viewModelScope.launch { _effect.emit(TaskListUiEffect.NavigateToCreate) }
    }
}
