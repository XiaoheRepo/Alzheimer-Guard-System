package com.xiaohelab.guard.android.feature.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientEditUiState(
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val medicalHistory: String = "",
    val characteristics: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface PatientEditUiEffect {
    object Success : PatientEditUiEffect
}

@HiltViewModel
class PatientEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val patientId: String? = savedStateHandle["patientId"]
    private val _state = MutableStateFlow(PatientEditUiState())
    val state: StateFlow<PatientEditUiState> = _state

    private val _effect = MutableSharedFlow<PatientEditUiEffect>()
    val effect: SharedFlow<PatientEditUiEffect> = _effect

    init {
        patientId?.let { loadPatient(it) }
    }

    private fun loadPatient(id: String) {
        viewModelScope.launch {
            when (val result = patientRepository.getPatientById(id)) {
                is ApiResult.Success -> {
                    val p = result.data
                    _state.update {
                        it.copy(
                            name = p.name,
                            age = p.age?.toString() ?: "",
                            gender = p.gender ?: "",
                            height = p.height?.toString() ?: "",
                            weight = p.weight?.toString() ?: "",
                            medicalHistory = p.medicalHistory ?: "",
                            characteristics = p.characteristics ?: ""
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onAgeChange(v: String) = _state.update { it.copy(age = v) }
    fun onGenderChange(v: String) = _state.update { it.copy(gender = v) }
    fun onHeightChange(v: String) = _state.update { it.copy(height = v) }
    fun onWeightChange(v: String) = _state.update { it.copy(weight = v) }
    fun onMedicalHistoryChange(v: String) = _state.update { it.copy(medicalHistory = v) }
    fun onCharacteristicsChange(v: String) = _state.update { it.copy(characteristics = v) }

    fun submit() {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "姓名不能为空") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = if (patientId == null) {
                patientRepository.createPatient(
                    s.name.trim(), s.age.toIntOrNull(), s.gender.ifBlank { null },
                    s.height.toIntOrNull(), s.weight.toIntOrNull(),
                    s.medicalHistory.ifBlank { null }, s.characteristics.ifBlank { null }
                )
            } else {
                patientRepository.updatePatient(
                    patientId, s.name.trim(), s.age.toIntOrNull(), s.gender.ifBlank { null },
                    s.height.toIntOrNull(), s.weight.toIntOrNull(),
                    s.medicalHistory.ifBlank { null }, s.characteristics.ifBlank { null }
                )
            }
            when (result) {
                is ApiResult.Success -> _effect.emit(PatientEditUiEffect.Success)
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
