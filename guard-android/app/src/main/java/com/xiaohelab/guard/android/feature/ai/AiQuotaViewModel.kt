package com.xiaohelab.guard.android.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.AiQuota
import com.xiaohelab.guard.android.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiQuotaUiState(
    val quota: AiQuota? = null,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AiQuotaViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AiQuotaUiState())
    val state: StateFlow<AiQuotaUiState> = _state

    init { loadQuota() }

    fun loadQuota() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = aiRepository.getQuota()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, quota = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
