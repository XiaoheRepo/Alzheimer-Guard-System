@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.task.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.xiaohelab.guard.android.core.eventbus.AppEvent
import com.xiaohelab.guard.android.core.eventbus.AppEventBus
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.task.data.RescueTaskDto
import com.xiaohelab.guard.android.feature.task.domain.CancelTaskUseCase
import com.xiaohelab.guard.android.feature.task.domain.CreateTaskUseCase
import com.xiaohelab.guard.android.feature.task.domain.GetTaskUseCase
import com.xiaohelab.guard.android.feature.task.domain.ListTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── TaskListViewModel ────────────────────────────────────────────────────────

data class TaskListUiState(
    val loading: Boolean = true,
    val tasks: List<RescueTaskDto> = emptyList(),
    val error: DomainException? = null,
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val listTasks: ListTasksUseCase,
    private val eventBus: AppEventBus,
) : ViewModel() {
    private val _s = MutableStateFlow(TaskListUiState())
    val state: StateFlow<TaskListUiState> = _s.asStateFlow()

    fun load(patientId: String? = null) {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = listTasks(patientId)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, tasks = r.data.items) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    fun observeWs() {
        viewModelScope.launch {
            eventBus.events.collect { ev ->
                /** HC-02: WS state.changed 到达时强制刷新，不本地推算 status。 */
                if (ev is AppEvent.StateChanged && ev.type.startsWith("rescue_task")) load()
            }
        }
    }
}

// ─── TaskDetailViewModel ──────────────────────────────────────────────────────

data class TaskDetailUiState(
    val loading: Boolean = true,
    val task: RescueTaskDto? = null,
    val error: DomainException? = null,
    val showCancelDialog: Boolean = false,
    val cancelling: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTask: GetTaskUseCase,
    private val cancelTask: CancelTaskUseCase,
    private val eventBus: AppEventBus,
) : ViewModel() {
    private val _s = MutableStateFlow(TaskDetailUiState())
    val state: StateFlow<TaskDetailUiState> = _s.asStateFlow()

    fun load(taskId: String) {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = getTask(taskId)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, task = r.data) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    /** HC-02: WS state.changed 到达时强制刷新，不本地推算 status。 */
    fun observeWs(taskId: String) {
        viewModelScope.launch {
            eventBus.events.collect { ev ->
                if (ev is AppEvent.StateChanged && ev.aggregateId == taskId) load(taskId)
            }
        }
    }

    fun requestCancel() = _s.update { it.copy(showCancelDialog = true) }
    fun dismissCancel() = _s.update { it.copy(showCancelDialog = false) }

    fun confirmCancel(taskId: String, onDone: () -> Unit) {
        _s.update { it.copy(showCancelDialog = false, cancelling = true) }
        viewModelScope.launch {
            when (val r = cancelTask(taskId)) {
                is MhResult.Success -> { _s.update { it.copy(cancelling = false) }; onDone() }
                is MhResult.Failure -> _s.update { it.copy(cancelling = false, error = r.error) }
            }
        }
    }
}

// ─── TaskCreateViewModel ──────────────────────────────────────────────────────

data class TaskCreateUiState(
    val patientId: String = "",
    val description: String = "",
    val submitting: Boolean = false,
    val error: DomainException? = null,
    val createdTaskId: String? = null,
)

@HiltViewModel
class TaskCreateViewModel @Inject constructor(
    private val createTask: CreateTaskUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(TaskCreateUiState())
    val state: StateFlow<TaskCreateUiState> = _s.asStateFlow()

    fun onPatientIdChange(v: String) = _s.update { it.copy(patientId = v, error = null) }
    fun onDescriptionChange(v: String) = _s.update { it.copy(description = v) }

    fun submit() {
        val s = _s.value
        if (s.patientId.isBlank()) return
        _s.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = createTask(s.patientId.trim(), s.description.ifBlank { null }, null)) {
                is MhResult.Success -> _s.update { it.copy(submitting = false, createdTaskId = r.data.taskId) }
                is MhResult.Failure -> _s.update { it.copy(submitting = false, error = r.error) }
            }
        }
    }
}

// ─── TaskListScreen (MH-TASK-00) ─────────────────────────────────────────────

/** MH-TASK-00: 寻回任务列表。 */
@Composable
fun TaskListScreen(
    onTaskClick: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    vm: TaskListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        vm.load()
        vm.observeWs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.task_create_title))
            }
        },
    ) { pad ->
        when {
            state.loading -> MhLoading(Modifier.padding(pad))
            state.error != null -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(ErrorMessageMapper.message(ctx, state.error!!), color = MaterialTheme.colorScheme.error)
            }
            state.tasks.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(stringResource(R.string.common_empty))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(pad),
            ) {
                items(state.tasks, key = { it.taskId }) { task ->
                    Card(onClick = { onTaskClick(task.taskId) }, modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(task.patientId) },
                            supportingContent = {
                                Text("${stringResource(R.string.task_field_status)}: ${task.status}")
                            },
                            trailingContent = {
                                Badge { Text(task.status) }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── TaskCreateScreen (MH-TASK-01) ───────────────────────────────────────────

/** MH-TASK-01: 发布寻回任务。 */
@Composable
fun TaskCreateScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    vm: TaskCreateViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(state.createdTaskId) {
        state.createdTaskId?.let { onCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_create_title)) },
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
                value = state.patientId,
                onValueChange = vm::onPatientIdChange,
                label = { Text("Patient ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::onDescriptionChange,
                label = { Text(stringResource(R.string.task_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            state.error?.let {
                Text(ErrorMessageMapper.message(ctx, it), color = MaterialTheme.colorScheme.error)
            }
            MhPrimaryButton(
                text = stringResource(R.string.common_submit),
                contentDesc = stringResource(R.string.task_create_title),
                onClick = vm::submit,
                enabled = !state.submitting,
            )
        }
    }
}

// ─── TaskDetailScreen (MH-TASK-02) ───────────────────────────────────────────

/**
 * MH-TASK-02: 任务详情。
 * HC-02: status 只读展示，WS state.changed 触发强制刷新，不本地推算。
 */
@Composable
fun TaskDetailScreen(
    taskId: String,
    onClues: () -> Unit,
    onBack: () -> Unit,
    vm: TaskDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(taskId) {
        vm.load(taskId)
        vm.observeWs(taskId)
    }

    if (state.showCancelDialog) {
        AlertDialog(
            onDismissRequest = vm::dismissCancel,
            title = { Text(stringResource(R.string.task_cancel)) },
            text = { Text(stringResource(R.string.task_cancel_confirm)) },
            confirmButton = {
                TextButton(onClick = { vm.confirmCancel(taskId, onBack) }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissCancel) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { pad ->
        when {
            state.loading -> MhLoading(Modifier.padding(pad))
            state.error != null -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(ErrorMessageMapper.message(ctx, state.error!!), color = MaterialTheme.colorScheme.error)
            }
            state.task != null -> {
                val task = state.task!!
                Column(
                    modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.task_field_status)) },
                        // HC-02: 只读展示，不推算
                        supportingContent = { Text(task.status) },
                    )
                    task.description?.let {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.task_field_description)) },
                            supportingContent = { Text(it) },
                        )
                    }
                    task.lastSeenLocation?.let { loc ->
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.task_field_last_seen)) },
                            supportingContent = { Text("${loc.lat}, ${loc.lng}${loc.address?.let { " ($it)" } ?: ""}") },
                        )
                    }
                    MhPrimaryButton(
                        text = stringResource(R.string.task_field_status) + " → 线索",
                        contentDesc = stringResource(R.string.clue_list_title),
                        onClick = onClues,
                    )
                    MhPrimaryButton(
                        text = stringResource(R.string.task_cancel),
                        contentDesc = stringResource(R.string.task_cancel),
                        onClick = vm::requestCancel,
                        enabled = !state.cancelling,
                    )
                }
            }
        }
    }
}
