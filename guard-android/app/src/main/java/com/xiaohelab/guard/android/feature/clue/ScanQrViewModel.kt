package com.xiaohelab.guard.android.feature.clue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.ResourceInfo
import com.xiaohelab.guard.android.domain.repository.ClueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanQrUiState(
    val scanning: Boolean = false,
    val resolving: Boolean = false,
    val error: String? = null
)

sealed interface ScanQrUiEffect {
    data class NavigateToReport(val resourceToken: String, val taskId: String?) : ScanQrUiEffect
    data class NavigateToManualEntry(val resourceToken: String) : ScanQrUiEffect
}

@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val clueRepository: ClueRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScanQrUiState())
    val state: StateFlow<ScanQrUiState> = _state

    private val _effect = MutableSharedFlow<ScanQrUiEffect>()
    val effect: SharedFlow<ScanQrUiEffect> = _effect

    /** 扫描到 token 后调用 */
    fun onTokenScanned(token: String) {
        viewModelScope.launch {
            _state.update { it.copy(resolving = true, error = null) }
            when (val result = clueRepository.resolveResourceToken(token)) {
                is ApiResult.Success -> {
                    val info = result.data
                    clueRepository.saveAnonymousToken(token)
                    if (info.taskId != null) {
                        _effect.emit(ScanQrUiEffect.NavigateToReport(token, info.taskId))
                    } else {
                        _effect.emit(ScanQrUiEffect.NavigateToManualEntry(token))
                    }
                    _state.update { it.copy(resolving = false) }
                }
                is ApiResult.Failure -> _state.update { it.copy(resolving = false, error = result.message) }
            }
        }
    }
}
