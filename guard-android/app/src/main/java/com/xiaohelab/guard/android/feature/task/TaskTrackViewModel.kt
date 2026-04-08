package com.xiaohelab.guard.android.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.TrajectoryPoint
import com.xiaohelab.guard.android.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskTrackUiState(
    val loading: Boolean = false,
    val points: List<TrajectoryPoint> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TaskTrackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])
    private val _state = MutableStateFlow(TaskTrackUiState())
    val state: StateFlow<TaskTrackUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = taskRepository.getLatestTrajectory(taskId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, points = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
