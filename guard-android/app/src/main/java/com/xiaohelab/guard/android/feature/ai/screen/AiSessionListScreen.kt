package com.xiaohelab.guard.android.feature.ai.screen

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
import com.xiaohelab.guard.android.domain.model.AiSession
import com.xiaohelab.guard.android.feature.ai.AiSessionListUiEffect
import com.xiaohelab.guard.android.feature.ai.AiSessionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSessionListScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: AiSessionListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AiSessionListUiEffect.NavigateToChat -> onNavigateToChat(effect.sessionId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 智能助手") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::createSession) {
                Text("新对话")
            }
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.sessions.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("暂无对话")
                    TextButton(onClick = viewModel::createSession) { Text("开始新对话") }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                items(state.sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onClick = { viewModel.openSession(session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: AiSession, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(session.title.ifBlank { "对话 ${session.id.take(8)}" }, style = MaterialTheme.typography.bodyLarge)
            Text("消息数：${session.messageCount}", style = MaterialTheme.typography.bodySmall)
            session.lastMessageAt?.let { Text("最后消息：$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
        }
    }
}
