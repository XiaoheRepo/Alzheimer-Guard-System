package com.xiaohelab.guard.android.feature.clue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.Clue
import com.xiaohelab.guard.android.domain.repository.ClueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClueDetailUiState(
    val loading: Boolean = false,
    val clue: Clue? = null,
    val error: String? = null
)

@HiltViewModel
class ClueDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clueRepository: ClueRepository
) : ViewModel() {

    private val clueId: String = checkNotNull(savedStateHandle["clueId"])
    private val _state = MutableStateFlow(ClueDetailUiState())
    val state: StateFlow<ClueDetailUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = clueRepository.getClueById(clueId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, clue = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
