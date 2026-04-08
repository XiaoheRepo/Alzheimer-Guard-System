package com.xiaohelab.guard.android.feature.home.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.feature.home.HomeUiEffect
import com.xiaohelab.guard.android.feature.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTask: (taskId: String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPatient: (patientId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeUiEffect.NavigateToLogin -> onNavigateToLogin()
                is HomeUiEffect.NavigateToTaskDetail -> onNavigateToTask(effect.taskId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("守护者") },
                actions = {
                    BadgedBox(
                        badge = {
                            if (state.unreadCount > 0) Badge { Text(state.unreadCount.toString()) }
                        }
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "通知")
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = viewModel::load,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                // 患者选择器
                if (state.patients.isNotEmpty()) {
                    item {
                        Text("选择患者", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.patients) { patient ->
                                FilterChip(
                                    selected = patient.id == state.selectedPatientId,
                                    onClick = { viewModel.selectPatient(patient.id) },
                                    label = { Text(patient.nameMasked) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }

                // 当前患者信息
                state.selectedPatient?.let { patient ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPatient(patient.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = patient.nameMasked,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                patient.age?.let {
                                    Text("${it}岁 · ${patient.gender ?: "未知"}")
                                }
                                patient.boundTagId?.let {
                                    Text("标签：$it", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 活跃救援任务
                if (state.activeTasks.isNotEmpty()) {
                    item {
                        Text(
                            "活跃救援任务（${state.activeTasks.size}）",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    items(state.activeTasks, key = { it.id }) { task ->
                        TaskCard(task = task, onClick = { onNavigateToTask(task.id) })
                    }
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

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "救援 · ${task.patientNameMasked}",
                    style = MaterialTheme.typography.titleSmall
                )
                task.remark?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    text = "发起：${task.startTime}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "进行中",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
