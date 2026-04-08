package com.xiaohelab.guard.android.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.TagOrder
import com.xiaohelab.guard.android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val order: TagOrder? = null,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface OrderDetailUiEffect {
    data class ShowToast(val message: String) : OrderDetailUiEffect
    object NavigateBack : OrderDetailUiEffect
}

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])
    private val _state = MutableStateFlow(OrderDetailUiState())
    val state: StateFlow<OrderDetailUiState> = _state

    private val _effect = MutableSharedFlow<OrderDetailUiEffect>()
    val effect: SharedFlow<OrderDetailUiEffect> = _effect

    init { loadOrder() }

    private fun loadOrder() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = orderRepository.getOrderById(orderId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, order = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun cancelOrder() {
        viewModelScope.launch {
            when (val result = orderRepository.cancelOrder(orderId)) {
                is ApiResult.Success -> {
                    _effect.emit(OrderDetailUiEffect.ShowToast("订单已取消"))
                    _effect.emit(OrderDetailUiEffect.NavigateBack)
                }
                is ApiResult.Failure -> _effect.emit(OrderDetailUiEffect.ShowToast(result.message))
            }
        }
    }
}
