package com.xiaohelab.guard.android.feature.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.Patient
import com.xiaohelab.guard.android.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUiState(
    val loading: Boolean = false,
    val patient: Patient? = null,
    val error: String? = null
)

sealed interface PatientDetailUiEffect {
    data class NavigateToEdit(val patientId: String) : PatientDetailUiEffect
    data class NavigateToFence(val patientId: String) : PatientDetailUiEffect
    data class NavigateToGuardians(val patientId: String) : PatientDetailUiEffect
    data class NavigateToTag(val patientId: String) : PatientDetailUiEffect
}

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])
    private val _state = MutableStateFlow(PatientDetailUiState())
    val state: StateFlow<PatientDetailUiState> = _state

    private val _effect = MutableSharedFlow<PatientDetailUiEffect>()
    val effect: SharedFlow<PatientDetailUiEffect> = _effect

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = patientRepository.getPatientById(patientId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, patient = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun editProfile() = viewModelScope.launch { _effect.emit(PatientDetailUiEffect.NavigateToEdit(patientId)) }
    fun editFence() = viewModelScope.launch { _effect.emit(PatientDetailUiEffect.NavigateToFence(patientId)) }
    fun manageGuardians() = viewModelScope.launch { _effect.emit(PatientDetailUiEffect.NavigateToGuardians(patientId)) }
    fun manageTag() = viewModelScope.launch { _effect.emit(PatientDetailUiEffect.NavigateToTag(patientId)) }
}
