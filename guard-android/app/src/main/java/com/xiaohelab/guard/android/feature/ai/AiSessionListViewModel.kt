package com.xiaohelab.guard.android.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.AppSettingsManager
import com.xiaohelab.guard.android.domain.model.AiSession
import com.xiaohelab.guard.android.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSessionListUiState(
    val sessions: List<AiSession> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface AiSessionListUiEffect {
    data class NavigateToChat(val sessionId: String) : AiSessionListUiEffect
}

@HiltViewModel
class AiSessionListViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(AiSessionListUiState())
    val state: StateFlow<AiSessionListUiState> = _state

    private val _effect = MutableSharedFlow<AiSessionListUiEffect>()
    val effect: SharedFlow<AiSessionListUiEffect> = _effect

    init { loadSessions() }

    fun loadSessions() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val patientId = appSettingsManager.currentPatientId.first()
            if (patientId == null) { _state.update { it.copy(loading = false, error = "请先选择患者") }; return@launch }
            when (val result = aiRepository.getSessions(patientId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, sessions = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun createSession() {
        viewModelScope.launch {
            val patientId = appSettingsManager.currentPatientId.first() ?: return@launch
            when (val result = aiRepository.createSession(patientId, null)) {
                is ApiResult.Success -> {
                    loadSessions()
                    _effect.emit(AiSessionListUiEffect.NavigateToChat(result.data.id))
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun openSession(sessionId: String) {
        viewModelScope.launch { _effect.emit(AiSessionListUiEffect.NavigateToChat(sessionId)) }
    }
}
