package com.xiaohelab.guard.android.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.AppSettingsManager
import com.xiaohelab.guard.android.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateTaskUiState(
    val patientId: String = "",
    val source: String = "MANUAL",
    val remark: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface CreateTaskUiEffect {
    data class NavigateToDetail(val taskId: String) : CreateTaskUiEffect
}

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val settingsManager: AppSettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(CreateTaskUiState())
    val state: StateFlow<CreateTaskUiState> = _state

    private val _effect = MutableSharedFlow<CreateTaskUiEffect>()
    val effect: SharedFlow<CreateTaskUiEffect> = _effect

    init {
        viewModelScope.launch {
            val pid = savedStateHandle.get<String>("patientId")
                ?: settingsManager.getCurrentPatientId()
            if (pid != null) _state.update { it.copy(patientId = pid) }
        }
    }

    fun onRemarkChange(v: String) = _state.update { it.copy(remark = v, error = null) }
    fun onSourceChange(v: String) = _state.update { it.copy(source = v) }

    fun submit() {
        val s = _state.value
        if (s.patientId.isBlank()) {
            _state.update { it.copy(error = "请先选择患者") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = taskRepository.createTask(s.patientId, s.source, s.remark.ifBlank { null })) {
                is ApiResult.Success -> _effect.emit(CreateTaskUiEffect.NavigateToDetail(result.data.id))
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
