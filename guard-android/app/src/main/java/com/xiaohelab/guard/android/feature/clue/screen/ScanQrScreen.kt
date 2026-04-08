package com.xiaohelab.guard.android.feature.clue.screen

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.feature.clue.ScanQrUiEffect
import com.xiaohelab.guard.android.feature.clue.ScanQrViewModel

/**
 * PUB-01 扫描二维码/NFC
 * 真实扫码由 ML Kit/ZXing 落地，此处用输入框模拟 token 输入以可编译
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReport: (token: String, taskId: String) -> Unit,
    onNavigateToManualEntry: (token: String) -> Unit,
    viewModel: ScanQrViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScanQrUiEffect.NavigateToReport ->
                    onNavigateToReport(effect.resourceToken, effect.taskId ?: "")
                is ScanQrUiEffect.NavigateToManualEntry ->
                    onNavigateToManualEntry(effect.resourceToken)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫描二维码") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("请将摄像头对准二维码", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(32.dp))
                if (state.resolving) {
                    CircularProgressIndicator()
                }
                state.error?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                // 开发调试：模拟扫码
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.onTokenScanned("mock_token_demo_12345") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("模拟扫码（调试）") }
            }
        }
    }
}
