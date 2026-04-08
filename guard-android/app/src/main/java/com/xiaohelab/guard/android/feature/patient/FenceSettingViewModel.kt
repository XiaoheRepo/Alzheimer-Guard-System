package com.xiaohelab.guard.android.feature.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.GeoFence
import com.xiaohelab.guard.android.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FenceSettingUiState(
    val centerLat: String = "",
    val centerLng: String = "",
    val radiusMeters: String = "200",
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface FenceSettingUiEffect {
    object Success : FenceSettingUiEffect
}

@HiltViewModel
class FenceSettingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])
    private val _state = MutableStateFlow(FenceSettingUiState())
    val state: StateFlow<FenceSettingUiState> = _state

    private val _effect = MutableSharedFlow<FenceSettingUiEffect>()
    val effect: SharedFlow<FenceSettingUiEffect> = _effect

    init {
        loadCurrentFence()
    }

    private fun loadCurrentFence() {
        viewModelScope.launch {
            when (val result = patientRepository.getPatientById(patientId)) {
                is ApiResult.Success -> {
                    result.data.fence?.let { f ->
                        _state.update {
                            it.copy(
                                centerLat = f.centerLat.toString(),
                                centerLng = f.centerLng.toString(),
                                radiusMeters = f.radiusMeters.toString(),
                                enabled = f.enabled
                            )
                        }
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun onLatChange(v: String) = _state.update { it.copy(centerLat = v) }
    fun onLngChange(v: String) = _state.update { it.copy(centerLng = v) }
    fun onRadiusChange(v: String) = _state.update { it.copy(radiusMeters = v) }
    fun onEnabledChange(v: Boolean) = _state.update { it.copy(enabled = v) }

    fun save() {
        val s = _state.value
        val lat = s.centerLat.toDoubleOrNull() ?: run { _state.update { it.copy(error = "纬度格式无效") }; return }
        val lng = s.centerLng.toDoubleOrNull() ?: run { _state.update { it.copy(error = "经度格式无效") }; return }
        val radius = s.radiusMeters.toIntOrNull()?.takeIf { it in 50..5000 }
            ?: run { _state.update { it.copy(error = "半径需在 50~5000 m 之间") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = patientRepository.updateFence(patientId, GeoFence(lat, lng, radius, s.enabled))) {
                is ApiResult.Success -> _effect.emit(FenceSettingUiEffect.Success)
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
