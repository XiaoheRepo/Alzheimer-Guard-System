package com.xiaohelab.guard.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.AppSettingsManager
import com.xiaohelab.guard.android.core.datastore.TokenManager
import com.xiaohelab.guard.android.domain.model.Patient
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.domain.model.TaskStatus
import com.xiaohelab.guard.android.domain.repository.PatientRepository
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

data class HomeUiState(
    val loading: Boolean = false,
    val patients: List<Patient> = emptyList(),
    val selectedPatientId: String? = null,
    val activeTasks: List<Task> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null
) {
    val selectedPatient: Patient? get() = patients.find { it.id == selectedPatientId }
}

sealed interface HomeUiEffect {
    object NavigateToLogin : HomeUiEffect
    data class NavigateToTaskDetail(val taskId: String) : HomeUiEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val taskRepository: TaskRepository,
    private val tokenManager: TokenManager,
    private val settingsManager: AppSettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private val _effect = MutableSharedFlow<HomeUiEffect>()
    val effect: SharedFlow<HomeUiEffect> = _effect

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val pResult = patientRepository.getMyPatients()) {
                is ApiResult.Success -> {
                    val patients = pResult.data
                    val savedId = settingsManager.currentPatientId.first()
                    val selectedId = patients.find { it.id == savedId }?.id
                        ?: patients.firstOrNull()?.id
                    _state.update { it.copy(patients = patients, selectedPatientId = selectedId) }
                    if (selectedId != null) loadTasks(selectedId)
                    else _state.update { it.copy(loading = false) }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = pResult.message)
                }
            }
        }
    }

    fun selectPatient(patientId: String) {
        viewModelScope.launch {
            settingsManager.setCurrentPatientId(patientId)
            _state.update { it.copy(selectedPatientId = patientId) }
            loadTasks(patientId)
        }
    }

    private suspend fun loadTasks(patientId: String) {
        when (val result = taskRepository.fetchTasks(patientId)) {
            is ApiResult.Success -> {
                val active = result.data.filter { it.status == TaskStatus.ACTIVE }
                _state.update { it.copy(loading = false, activeTasks = active) }
            }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearSession()
            _effect.emit(HomeUiEffect.NavigateToLogin)
        }
    }
}
