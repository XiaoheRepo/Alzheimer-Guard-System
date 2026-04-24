@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.clue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.clue.data.ClueDto
import com.xiaohelab.guard.android.feature.clue.domain.CreateClueUseCase
import com.xiaohelab.guard.android.feature.clue.domain.ListCluesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ClueListViewModel ────────────────────────────────────────────────────────

data class ClueListUiState(
    val loading: Boolean = true,
    val clues: List<ClueDto> = emptyList(),
    val error: DomainException? = null,
)

@HiltViewModel
class ClueListViewModel @Inject constructor(
    private val listClues: ListCluesUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(ClueListUiState())
    val state: StateFlow<ClueListUiState> = _s.asStateFlow()

    fun load(taskId: String) {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = listClues(taskId)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, clues = r.data.items) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

// ─── ClueCreateViewModel ──────────────────────────────────────────────────────

data class ClueCreateUiState(
    val type: String = "TEXT",
    val content: String = "",
    val submitting: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class ClueCreateViewModel @Inject constructor(
    private val createClue: CreateClueUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(ClueCreateUiState())
    val state: StateFlow<ClueCreateUiState> = _s.asStateFlow()

    fun onTypeChange(v: String) = _s.update { it.copy(type = v) }
    fun onContentChange(v: String) = _s.update { it.copy(content = v, error = null) }

    fun submit(taskId: String) {
        val s = _s.value
        if (s.content.isBlank()) return
        _s.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            // HC-Coord: 暂不携带坐标；如需位置则附 coord_system="GCJ-02"
            when (val r = createClue(taskId, s.type, s.content.trim(), null)) {
                is MhResult.Success -> _s.update { it.copy(submitting = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(submitting = false, error = r.error) }
            }
        }
    }
}

// ─── ClueListScreen (MH-CLUE-00) ─────────────────────────────────────────────

/** MH-CLUE-00: 线索列表。HC-02 review_state 只读展示。 */
@Composable
fun ClueListScreen(
    taskId: String,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    vm: ClueListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(taskId) { vm.load(taskId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clue_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.clue_create_title))
            }
        },
    ) { pad ->
        when {
            state.loading -> MhLoading(Modifier.padding(pad))
            state.error != null -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(ErrorMessageMapper.message(ctx, state.error!!), color = MaterialTheme.colorScheme.error)
            }
            state.clues.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(stringResource(R.string.common_empty))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(pad),
            ) {
                items(state.clues, key = { it.clueId }) { clue ->
                    Card(Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(clue.content.take(60)) },
                            supportingContent = {
                                Column {
                                    Text("${stringResource(R.string.clue_review_state)}: ${clue.reviewState}")
                                    Text(clue.type)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── ClueCreateScreen (MH-CLUE-01) ───────────────────────────────────────────

/** MH-CLUE-01: 提交线索表单。 */
@Composable
fun ClueCreateScreen(
    taskId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: ClueCreateViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(state.success) { if (state.success) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clue_create_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.type,
                onValueChange = vm::onTypeChange,
                label = { Text(stringResource(R.string.clue_field_type)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.content,
                onValueChange = vm::onContentChange,
                label = { Text(stringResource(R.string.clue_field_content)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            state.error?.let {
                Text(ErrorMessageMapper.message(ctx, it), color = MaterialTheme.colorScheme.error)
            }
            MhPrimaryButton(
                text = stringResource(R.string.clue_submit),
                contentDesc = stringResource(R.string.clue_submit),
                onClick = { vm.submit(taskId) },
                enabled = !state.submitting,
            )
        }
    }
}
