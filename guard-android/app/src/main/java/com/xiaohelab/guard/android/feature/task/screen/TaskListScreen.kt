package com.xiaohelab.guard.android.feature.task.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.domain.model.TaskStatus
import com.xiaohelab.guard.android.feature.task.TaskListUiEffect
import com.xiaohelab.guard.android.feature.task.TaskListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TaskListUiEffect.NavigateToDetail -> onNavigateToDetail(effect.taskId)
                is TaskListUiEffect.NavigateToCreate -> onNavigateToCreate()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("救援任务") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreate) {
                Icon(Icons.Default.Add, "发起救援")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 状态过滤器
            val filters = listOf(null to "全部", TaskStatus.ACTIVE to "进行中",
                TaskStatus.RESOLVED to "已结束", TaskStatus.FALSE_ALARM to "虚假警报")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (status, label) ->
                    FilterChip(
                        selected = state.filterStatus == status,
                        onClick = { viewModel.setFilter(status) },
                        label = { Text(label) }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::load
            ) {
                val displayed = if (state.filterStatus == null) state.tasks
                else state.tasks.filter { it.status == state.filterStatus }

                if (displayed.isEmpty() && !state.loading) {
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text("暂无任务") }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayed, key = { it.id }) { task ->
                        TaskListItem(task = task, onClick = { viewModel.openDetail(task.id) })
                    }
                    if (state.error != null) {
                        item {
                            Text(
                                text = state.error!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListItem(task: Task, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.patientNameMasked, style = MaterialTheme.typography.titleSmall)
                Text(task.startTime, style = MaterialTheme.typography.bodySmall)
                task.remark?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.width(8.dp))
            val (label, color) = when (task.status) {
                TaskStatus.ACTIVE -> "进行中" to MaterialTheme.colorScheme.error
                TaskStatus.RESOLVED -> "已找到" to MaterialTheme.colorScheme.primary
                TaskStatus.FALSE_ALARM -> "虚假" to MaterialTheme.colorScheme.outline
                TaskStatus.UNKNOWN -> "未知" to MaterialTheme.colorScheme.outline
            }
            Text(label, color = color, style = MaterialTheme.typography.labelSmall)
        }
    }
}
