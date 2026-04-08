package com.xiaohelab.guard.android.feature.clue.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.feature.clue.ClueDetailViewModel

/** CLUE-01 线索详情 + PUB-05 回执 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClueDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClueDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("线索详情") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Column(Modifier.padding(24.dp)) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::load) { Text("重试") }
                }
                state.clue != null -> {
                    val clue = state.clue!!
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("线索 ID：${clue.id}", style = MaterialTheme.typography.labelSmall)
                        Text("状态：${clue.status.name}")
                        Text("描述：${clue.description}")
                        clue.locationDesc?.let { Text("位置：$it") }
                        clue.contactPhone?.let { Text("联系：$it") }
                        Text("提交时间：${clue.submittedAt}")
                    }
                }
            }
        }
    }
}

/** PUB-05 回执（展示成功提交的线索信息，复用 ClueDetail 页面逻辑） */
@Composable
fun ClueReceiptScreen(
    clueId: String,
    onNavigateHome: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("线索已提交！", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("感谢您的帮助，我们将跟进处理。", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onViewDetail(clueId) }, modifier = Modifier.fillMaxWidth()) {
            Text("查看线索详情")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth()) {
            Text("返回首页")
        }
    }
}
