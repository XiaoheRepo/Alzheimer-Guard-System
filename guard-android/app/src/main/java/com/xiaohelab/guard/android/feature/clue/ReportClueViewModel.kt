package com.xiaohelab.guard.android.feature.clue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.repository.ClueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportClueUiState(
    val description: String = "",
    val contactPhone: String = "",
    val locationDesc: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface ReportClueUiEffect {
    data class NavigateToReceipt(val clueId: String) : ReportClueUiEffect
}

@HiltViewModel
class ReportClueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clueRepository: ClueRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _state = MutableStateFlow(ReportClueUiState())
    val state: StateFlow<ReportClueUiState> = _state

    private val _effect = MutableSharedFlow<ReportClueUiEffect>()
    val effect: SharedFlow<ReportClueUiEffect> = _effect

    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v, error = null) }
    fun onContactPhoneChange(v: String) = _state.update { it.copy(contactPhone = v) }
    fun onLocationDescChange(v: String) = _state.update { it.copy(locationDesc = v) }

    fun submit() {
        val s = _state.value
        if (s.description.isBlank()) {
            _state.update { it.copy(error = "请输入线索描述") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = clueRepository.reportClue(
                taskId = taskId,
                description = s.description.trim(),
                locationDesc = s.locationDesc.ifBlank { null },
                lat = null, lng = null,
                contactPhone = s.contactPhone.ifBlank { null },
                images = emptyList()
            )) {
                is ApiResult.Success -> _effect.emit(ReportClueUiEffect.NavigateToReceipt(result.data.id))
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
