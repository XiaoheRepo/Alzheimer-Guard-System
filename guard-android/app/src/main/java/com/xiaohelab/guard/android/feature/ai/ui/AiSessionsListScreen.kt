@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.ai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhEmpty
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.feature.ai.data.AiSessionDto
import com.xiaohelab.guard.android.feature.ai.domain.DeleteAiSessionUseCase
import com.xiaohelab.guard.android.feature.ai.domain.ListAiSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSessionsUiState(
    val loading: Boolean = true,
    val sessions: List<AiSessionDto> = emptyList(),
    val error: DomainException? = null,
    val pendingDelete: AiSessionDto? = null,
)

@HiltViewModel
class AiSessionsListViewModel @Inject constructor(
    private val listSessions: ListAiSessionsUseCase,
    private val deleteSession: DeleteAiSessionUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(AiSessionsUiState())
    val state: StateFlow<AiSessionsUiState> = _s.asStateFlow()

    fun refresh() {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = listSessions()) {
                is MhResult.Success -> _s.update { it.copy(loading = false, sessions = r.data.items) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    fun askDelete(s: AiSessionDto) = _s.update { it.copy(pendingDelete = s) }
    fun dismissDelete() = _s.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val target = _s.value.pendingDelete ?: return
        _s.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            when (val r = deleteSession(target.sessionId)) {
                is MhResult.Success -> refresh()
                is MhResult.Failure -> _s.update { it.copy(error = r.error) }
            }
        }
    }
}

/** MH-AI-01: AI 对话历史会话列表。FAB 新建；点击条目进入对话；垃圾桶删除。 */
@Composable
fun AiSessionsListScreen(
    onOpenSession: (String?) -> Unit,
    vm: AiSessionsListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.ai_session_list_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenSession(null) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ai_session_new))
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                s.loading -> MhLoading()
                s.error != null -> Text(
                    ErrorMessageMapper.message(ctx, s.error!!),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                s.sessions.isEmpty() -> MhEmpty(stringResource(R.string.ai_session_empty))
                else -> LazyColumn {
                    items(s.sessions, key = { it.sessionId }) { item ->
                        ListItem(
                            headlineContent = { Text(item.title ?: item.sessionId) },
                            supportingContent = { Text(item.createdAt ?: item.status) },
                            trailingContent = {
                                IconButton(onClick = { vm.askDelete(item) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.ai_session_delete))
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClickLabel = stringResource(R.string.ai_session_open)) {
                                    onOpenSession(item.sessionId)
                                },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    s.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = vm::dismissDelete,
            title = { Text(stringResource(R.string.ai_session_delete)) },
            text = { Text(stringResource(R.string.ai_session_delete_confirm, target.title ?: target.sessionId)) },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDelete) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
