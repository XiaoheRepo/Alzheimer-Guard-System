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

data class ManualEntryUiState(
    val name: String = "",
    val phone: String = "",
    val description: String = "",
    val locationDesc: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface ManualEntryUiEffect {
    data class NavigateToReceipt(val clueId: String) : ManualEntryUiEffect
}

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clueRepository: ClueRepository
) : ViewModel() {

    private val resourceToken: String = checkNotNull(savedStateHandle["resourceToken"])

    private val _state = MutableStateFlow(ManualEntryUiState())
    val state: StateFlow<ManualEntryUiState> = _state

    private val _effect = MutableSharedFlow<ManualEntryUiEffect>()
    val effect: SharedFlow<ManualEntryUiEffect> = _effect

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v, error = null) }
    fun onLocationDescChange(v: String) = _state.update { it.copy(locationDesc = v) }

    fun submit() {
        val s = _state.value
        if (s.description.isBlank()) {
            _state.update { it.copy(error = "请输入线索描述") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val anonToken = clueRepository.getAnonymousToken() ?: resourceToken
            when (val result = clueRepository.submitManualClue(
                anonymousToken = anonToken,
                name = s.name.ifBlank { null },
                phone = s.phone.ifBlank { null },
                description = s.description.trim(),
                locationDesc = s.locationDesc.ifBlank { null },
                lat = null, lng = null,
                images = emptyList()
            )) {
                is ApiResult.Success -> _effect.emit(ManualEntryUiEffect.NavigateToReceipt(result.data.id))
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
