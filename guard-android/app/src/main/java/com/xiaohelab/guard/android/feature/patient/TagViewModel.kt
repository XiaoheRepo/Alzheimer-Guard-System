package com.xiaohelab.guard.android.feature.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.CreateTagOrderRequest
import com.xiaohelab.guard.android.domain.model.Patient
import com.xiaohelab.guard.android.domain.repository.OrderRepository
import com.xiaohelab.guard.android.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagUiState(
    val patient: Patient? = null,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface TagUiEffect {
    data class ShowToast(val message: String) : TagUiEffect
}

@HiltViewModel
class TagViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepository: PatientRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])
    private val _state = MutableStateFlow(TagUiState())
    val state: StateFlow<TagUiState> = _state

    private val _effect = MutableSharedFlow<TagUiEffect>()
    val effect: SharedFlow<TagUiEffect> = _effect

    init { loadPatient() }

    fun loadPatient() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = patientRepository.getPatientById(patientId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, patient = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun createTagOrder(shippingAddress: String) {
        if (shippingAddress.isBlank()) { _state.update { it.copy(error = "请输入收货地址") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val req = CreateTagOrderRequest(
                patientId = patientId,
                tagSku = "GUARD_TAG_V1",
                quantity = 1,
                shippingAddress = shippingAddress
            )
            when (val result = orderRepository.createOrder(req)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false) }
                    _effect.emit(TagUiEffect.ShowToast("购标签订单已创建"))
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
