package com.xiaohelab.guard.android.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.datastore.AppSettingsManager
import com.xiaohelab.guard.android.domain.model.CreateTagOrderRequest
import com.xiaohelab.guard.android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateOrderUiState(
    val shippingAddress: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface CreateOrderUiEffect {
    object Success : CreateOrderUiEffect
}

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(CreateOrderUiState())
    val state: StateFlow<CreateOrderUiState> = _state

    private val _effect = MutableSharedFlow<CreateOrderUiEffect>()
    val effect: SharedFlow<CreateOrderUiEffect> = _effect

    fun onShippingAddressChange(v: String) = _state.update { it.copy(shippingAddress = v) }

    fun createOrder() {
        val address = _state.value.shippingAddress.trim()
        if (address.isBlank()) { _state.update { it.copy(error = "请输入收货地址") }; return }
        viewModelScope.launch {
            val patientId = appSettingsManager.currentPatientId.first()
                ?: run { _state.update { it.copy(error = "请先选择患者") }; return@launch }
            _state.update { it.copy(loading = true, error = null) }
            val req = CreateTagOrderRequest(
                patientId = patientId,
                tagSku = "GUARD_TAG_V1",
                quantity = 1,
                shippingAddress = address
            )
            when (val result = orderRepository.createOrder(req)) {
                is ApiResult.Success -> _effect.emit(CreateOrderUiEffect.Success)
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
