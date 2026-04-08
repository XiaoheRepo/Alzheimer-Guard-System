package com.xiaohelab.guard.android.feature.order.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.OrderStatus
import com.xiaohelab.guard.android.domain.model.TagOrder
import com.xiaohelab.guard.android.feature.order.OrderListUiEffect
import com.xiaohelab.guard.android.feature.order.OrderListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: OrderListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OrderListUiEffect.NavigateToDetail -> onNavigateToDetail(effect.orderId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的订单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNavigateToCreate) { Text("新建订单") }
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.orders.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无订单")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                items(state.orders, key = { it.id }) { order ->
                    OrderCard(order = order, onClick = { viewModel.openOrder(order.id) })
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: TagOrder, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("订单 #${order.id.take(8)}", style = MaterialTheme.typography.bodyLarge)
                Text(order.status.displayName(), style = MaterialTheme.typography.labelMedium)
            }
            Text("标签 SKU：${order.tagSku}", style = MaterialTheme.typography.bodySmall)
            Text("数量：${order.quantity} | 金额：¥${order.totalAmount / 100.0}", style = MaterialTheme.typography.bodySmall)
            Text(order.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
