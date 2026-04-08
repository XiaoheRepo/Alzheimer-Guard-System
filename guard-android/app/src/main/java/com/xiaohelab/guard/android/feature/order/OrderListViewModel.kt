package com.xiaohelab.guard.android.feature.order

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

data class OrderListUiState(
    val orders: List<TagOrder> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface OrderListUiEffect {
    data class NavigateToDetail(val orderId: String) : OrderListUiEffect
}

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrderListUiState())
    val state: StateFlow<OrderListUiState> = _state

    private val _effect = MutableSharedFlow<OrderListUiEffect>()
    val effect: SharedFlow<OrderListUiEffect> = _effect

    init { loadOrders() }

    fun loadOrders() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = orderRepository.getOrders()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, orders = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun openOrder(orderId: String) {
        viewModelScope.launch { _effect.emit(OrderListUiEffect.NavigateToDetail(orderId)) }
    }
}
