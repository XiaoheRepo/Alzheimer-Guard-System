package com.xiaohelab.guard.android.feature.task.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.CloseTaskRequest
import com.xiaohelab.guard.android.feature.task.CloseTaskUiEffect
import com.xiaohelab.guard.android.feature.task.CloseTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: CloseTaskViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CloseTaskUiEffect.Success -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关闭任务") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("结案原因", style = MaterialTheme.typography.titleSmall)

            val reasons = listOf(
                CloseTaskRequest.CloseReason.FOUND to "已找到",
                CloseTaskRequest.CloseReason.FALSE_ALARM to "虚假警报"
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                reasons.forEachIndexed { index, (reason, label) ->
                    SegmentedButton(
                        selected = state.reason == reason,
                        onClick = { viewModel.onReasonChange(reason) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = reasons.size)
                    ) { Text(label) }
                }
            }

            if (state.reason == CloseTaskRequest.CloseReason.FALSE_ALARM) {
                OutlinedTextField(
                    value = state.remarks,
                    onValueChange = viewModel::onRemarksChange,
                    label = { Text("说明（5-256 字）") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::submit,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                else Text("确认关闭")
            }
        }
    }
}
