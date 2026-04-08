package com.xiaohelab.guard.android.feature.order.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.OrderStatus
import com.xiaohelab.guard.android.feature.order.OrderDetailUiEffect
import com.xiaohelab.guard.android.feature.order.OrderDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OrderDetailUiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is OrderDetailUiEffect.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("订单详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.order == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "加载失败")
            }
            else -> {
                val order = state.order!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("订单号：${order.id}", style = MaterialTheme.typography.bodyMedium)
                            Text("状态：${order.status.displayName()}", style = MaterialTheme.typography.bodyMedium)
                            Text("标签 SKU：${order.tagSku}")
                            Text("数量：${order.quantity}")
                            Text("金额：¥${order.totalAmount / 100.0}")
                            order.shippingAddress?.let { Text("收货地址：$it") }
                            order.trackingNumber?.let { Text("快递单号：$it") }
                            Text("下单时间：${order.createdAt}", style = MaterialTheme.typography.bodySmall)
                            order.paidAt?.let { Text("支付时间：$it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    if (order.status == OrderStatus.PENDING_PAYMENT) {
                        Button(
                            onClick = viewModel::cancelOrder,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("取消订单") }
                    }
                }
            }
        }
    }
}

private fun OrderStatus.displayName() = when (this) {
    OrderStatus.PENDING_PAYMENT -> "待支付"
    OrderStatus.PAID -> "已支付"
    OrderStatus.SHIPPED -> "已发货"
    OrderStatus.DELIVERED -> "已送达"
    OrderStatus.CANCELLED -> "已取消"
    OrderStatus.UNKNOWN -> "未知"
}
