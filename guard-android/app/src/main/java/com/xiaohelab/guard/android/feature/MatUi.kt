@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.mat.ui

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.mat.data.MaterialOrderDto
import com.xiaohelab.guard.android.feature.mat.domain.CreateMaterialOrderUseCase
import com.xiaohelab.guard.android.feature.mat.domain.ListMaterialOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── MaterialOrderListViewModel ──────────────────────────────────────────────

data class MatOrderListUiState(
    val loading: Boolean = true,
    val orders: List<MaterialOrderDto> = emptyList(),
    val error: DomainException? = null,
)

@HiltViewModel
class MaterialOrderListViewModel @Inject constructor(
    private val listOrders: ListMaterialOrdersUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(MatOrderListUiState())
    val state: StateFlow<MatOrderListUiState> = _s.asStateFlow()

    fun load(patientId: String) {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = listOrders(patientId)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, orders = r.data.items) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

// ─── MaterialOrderCreateViewModel ────────────────────────────────────────────

data class MatOrderCreateUiState(
    val itemCode: String = "",
    val quantity: String = "1",
    val deliveryAddress: String = "",
    val submitting: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class MaterialOrderCreateViewModel @Inject constructor(
    private val createOrder: CreateMaterialOrderUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(MatOrderCreateUiState())
    val state: StateFlow<MatOrderCreateUiState> = _s.asStateFlow()

    fun onItemCodeChange(v: String) = _s.update { it.copy(itemCode = v, error = null) }
    fun onQuantityChange(v: String) = _s.update { it.copy(quantity = v) }
    fun onDeliveryAddressChange(v: String) = _s.update { it.copy(deliveryAddress = v) }

    fun submit(patientId: String) {
        val s = _s.value
        val qty = s.quantity.toIntOrNull() ?: 1
        if (s.itemCode.isBlank()) return
        _s.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = createOrder(patientId, s.itemCode.trim(), qty, s.deliveryAddress.ifBlank { null })) {
                is MhResult.Success -> _s.update { it.copy(submitting = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(submitting = false, error = r.error) }
            }
        }
    }
}

// ─── MaterialOrderListScreen (MH-MAT-00) ─────────────────────────────────────

/** MH-MAT-00: 物资申领订单列表。HC-02 state 只读展示。 */
@Composable
fun MaterialOrderListScreen(
    patientId: String,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    vm: MaterialOrderListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(patientId) { vm.load(patientId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mat_order_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.mat_order_create_title))
            }
        },
    ) { pad ->
        when {
            state.loading -> MhLoading(Modifier.padding(pad))
            state.error != null -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(ErrorMessageMapper.message(ctx, state.error!!), color = MaterialTheme.colorScheme.error)
            }
            state.orders.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(stringResource(R.string.common_empty))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(pad),
            ) {
                items(state.orders, key = { it.orderId }) { order ->
                    Card(Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(order.itemCode) },
                            supportingContent = {
                                Column {
                                    Text("${stringResource(R.string.mat_order_state)}: ${order.state}")
                                    Text("x${order.quantity}")
                                    order.deliveryAddress?.let { Text(it) }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── MaterialOrderCreateScreen (MH-MAT-01) ───────────────────────────────────

/**
 * MH-MAT-01: 申领物资表单。
 * HC-07: 启用 FLAG_SECURE 防止截图。
 */
@Composable
fun MaterialOrderCreateScreen(
    patientId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: MaterialOrderCreateViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    // HC-07: FLAG_SECURE — 物资申领属于敏感操作
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (ctx as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    LaunchedEffect(state.success) { if (state.success) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mat_order_create_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.itemCode,
                onValueChange = vm::onItemCodeChange,
                label = { Text(stringResource(R.string.mat_field_item_code)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.quantity,
                onValueChange = vm::onQuantityChange,
                label = { Text(stringResource(R.string.mat_field_quantity)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.deliveryAddress,
                onValueChange = vm::onDeliveryAddressChange,
                label = { Text(stringResource(R.string.mat_field_delivery_address)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            state.error?.let {
                Text(ErrorMessageMapper.message(ctx, it), color = MaterialTheme.colorScheme.error)
            }
            MhPrimaryButton(
                text = stringResource(R.string.mat_order_submit),
                contentDesc = stringResource(R.string.mat_order_submit),
                onClick = { vm.submit(patientId) },
                enabled = !state.submitting,
            )
        }
    }
}
