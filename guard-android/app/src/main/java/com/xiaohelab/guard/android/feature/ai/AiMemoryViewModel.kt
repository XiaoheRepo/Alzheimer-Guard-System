package com.xiaohelab.guard.android.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.AppSettingsManager
import com.xiaohelab.guard.android.domain.model.AiMemoryNote
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

data class AiMemoryUiState(
    val notes: List<AiMemoryNote> = emptyList(),
    val newNoteContent: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface AiMemoryUiEffect {
    data class ShowToast(val message: String) : AiMemoryUiEffect
}

@HiltViewModel
class AiMemoryViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(AiMemoryUiState())
    val state: StateFlow<AiMemoryUiState> = _state

    private val _effect = MutableSharedFlow<AiMemoryUiEffect>()
    val effect: SharedFlow<AiMemoryUiEffect> = _effect

    init { loadNotes() }

    fun loadNotes() {
        viewModelScope.launch {
            val patientId = appSettingsManager.currentPatientId.first() ?: return@launch
            _state.update { it.copy(loading = true, error = null) }
            when (val result = aiRepository.getMemoryNotes(patientId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, notes = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onNewNoteContentChange(v: String) = _state.update { it.copy(newNoteContent = v) }

    fun addNote() {
        val content = _state.value.newNoteContent.trim()
        if (content.isBlank()) return
        viewModelScope.launch {
            val patientId = appSettingsManager.currentPatientId.first() ?: return@launch
            when (val result = aiRepository.createMemoryNote(patientId, content)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(newNoteContent = "") }
                    loadNotes()
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            when (val result = aiRepository.deleteMemoryNote(noteId)) {
                is ApiResult.Success -> { _effect.emit(AiMemoryUiEffect.ShowToast("已删除")); loadNotes() }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }
}
