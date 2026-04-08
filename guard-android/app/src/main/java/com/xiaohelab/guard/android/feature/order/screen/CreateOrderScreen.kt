package com.xiaohelab.guard.android.feature.order.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.feature.order.CreateOrderUiEffect
import com.xiaohelab.guard.android.feature.order.CreateOrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateOrderUiEffect.Success -> { Toast.makeText(context, "订单创建成功", Toast.LENGTH_SHORT).show(); onSuccess() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建标签订单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("标签型号：GUARD_TAG_V1", style = MaterialTheme.typography.bodyMedium)
            Text("数量：1件", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = state.shippingAddress,
                onValueChange = viewModel::onShippingAddressChange,
                label = { Text("收货地址*") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::createOrder,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("提交订单")
            }
        }
    }
}
