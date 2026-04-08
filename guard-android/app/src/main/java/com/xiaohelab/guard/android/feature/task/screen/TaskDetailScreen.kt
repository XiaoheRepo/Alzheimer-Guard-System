package com.xiaohelab.guard.android.feature.task.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.TaskStatus
import com.xiaohelab.guard.android.feature.task.TaskDetailUiEffect
import com.xiaohelab.guard.android.feature.task.TaskDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToClose: (String) -> Unit,
    onNavigateToTrack: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TaskDetailUiEffect.NavigateToClose -> onNavigateToClose(effect.taskId)
                is TaskDetailUiEffect.NavigateToTrack -> onNavigateToTrack(effect.taskId)
                is TaskDetailUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> CircularProgressIndicator(
                modifier = Modifier.padding(padding).padding(24.dp)
            )
            state.task == null -> Text(
                "任务不存在", modifier = Modifier.padding(padding).padding(24.dp)
            )
            else -> {
                val task = state.task!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Column {
                            Text("患者：${task.patientNameMasked}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("状态：${task.status.name}")
                            Text("来源：${task.source.name}")
                            Text("发起时间：${task.startTime}")
                            task.latestEventTime?.let { Text("最新进展：$it") }
                            task.remark?.let { Text("备注：$it") }
                            Spacer(Modifier.height(16.dp))

                            if (!task.status.isTerminal()) {
                                Button(
                                    onClick = viewModel::trackTask,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("查看轨迹") }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = viewModel::closeTask,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("关闭任务") }
                            }

                            Spacer(Modifier.height(16.dp))
                            Divider()
                            Text("事件记录", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    items(state.events, key = { it.eventId }) { event ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(event.description, style = MaterialTheme.typography.bodyMedium)
                            Text(event.occurredAt, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
