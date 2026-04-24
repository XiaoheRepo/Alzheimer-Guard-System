@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.tag.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xiaohelab.guard.android.feature.tag.data.TagDto
import com.xiaohelab.guard.android.feature.tag.domain.BindTagUseCase
import com.xiaohelab.guard.android.feature.tag.domain.ListTagsUseCase
import com.xiaohelab.guard.android.feature.tag.domain.UnbindTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── TagListViewModel ───────────────────────────────────────────────────────

data class TagListUiState(
    val loading: Boolean = true,
    val tags: List<TagDto> = emptyList(),
    val error: DomainException? = null,
    val unbinding: String? = null,         // tag_code being unbound
    val unbindConfirmTag: TagDto? = null,  // tag pending confirm dialog
)

@HiltViewModel
class TagListViewModel @Inject constructor(
    private val listTags: ListTagsUseCase,
    private val unbindTag: UnbindTagUseCase,
    private val eventBus: AppEventBus,
) : ViewModel() {
    private val _s = MutableStateFlow(TagListUiState())
    val state: StateFlow<TagListUiState> = _s.asStateFlow()

    fun load(patientId: String) {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = listTags(patientId)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, tags = r.data.items) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    /** HC-02: WS state.changed 触发刷新，不本地推算状态。 */
    fun observeWs(patientId: String) {
        viewModelScope.launch {
            eventBus.events.collect { ev ->
                if (ev is AppEvent.StateChanged && ev.type.startsWith("tag")) load(patientId)
            }
        }
    }

    fun requestUnbind(tag: TagDto) = _s.update { it.copy(unbindConfirmTag = tag) }
    fun dismissUnbindDialog() = _s.update { it.copy(unbindConfirmTag = null) }

    fun confirmUnbind(patientId: String, tagCode: String) {
        _s.update { it.copy(unbindConfirmTag = null, unbinding = tagCode) }
        viewModelScope.launch {
            when (val r = unbindTag(patientId, tagCode)) {
                is MhResult.Success -> load(patientId)
                is MhResult.Failure -> _s.update { it.copy(error = r.error, unbinding = null) }
            }
        }
    }
}

// ─── TagBindViewModel ────────────────────────────────────────────────────────

data class TagBindUiState(
    val tagCode: String = "",
    val deviceType: String = "",
    val alias: String = "",
    val submitting: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class TagBindViewModel @Inject constructor(
    private val bindTag: BindTagUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(TagBindUiState())
    val state: StateFlow<TagBindUiState> = _s.asStateFlow()

    fun prefill(tagCode: String) = _s.update { it.copy(tagCode = tagCode) }
    fun onTagCodeChange(v: String) = _s.update { it.copy(tagCode = v, error = null) }
    fun onDeviceTypeChange(v: String) = _s.update { it.copy(deviceType = v, error = null) }
    fun onAliasChange(v: String) = _s.update { it.copy(alias = v) }

    fun submit(patientId: String) {
        val s = _s.value
        if (s.tagCode.isBlank() || s.deviceType.isBlank()) return
        _s.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = bindTag(patientId, s.tagCode.trim(), s.deviceType.trim(), s.alias.ifBlank { null })) {
                is MhResult.Success -> _s.update { it.copy(submitting = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(submitting = false, error = r.error) }
            }
        }
    }
}

// ─── TagListScreen (MH-TAG-00) ────────────────────────────────────────────────

/** MH-TAG-00: 患者标签列表。HC-02 状态只读展示，不本地推算。 */
@Composable
fun TagListScreen(
    patientId: String,
    onBind: () -> Unit,
    onBack: () -> Unit,
    vm: TagListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(patientId) {
        vm.load(patientId)
        vm.observeWs(patientId)
    }

    // Unbind confirm dialog
    state.unbindConfirmTag?.let { tag ->
        AlertDialog(
            onDismissRequest = vm::dismissUnbindDialog,
            title = { Text(stringResource(R.string.tag_unbind)) },
            text = { Text(stringResource(R.string.tag_unbind_confirm)) },
            confirmButton = {
                TextButton(onClick = { vm.confirmUnbind(patientId, tag.tagCode) }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissUnbindDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tag_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onBind) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tag_bind_title))
            }
        },
    ) { pad ->
        when {
            state.loading -> MhLoading(Modifier.padding(pad))
            state.error != null -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(ErrorMessageMapper.message(ctx, state.error!!), color = MaterialTheme.colorScheme.error)
            }
            state.tags.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(stringResource(R.string.common_empty))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(pad),
            ) {
                items(state.tags, key = { it.tagCode }) { tag ->
                    Card(Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(tag.tagCode) },
                            supportingContent = {
                                Column {
                                    Text("${stringResource(R.string.tag_state)}: ${tag.state}")
                                    tag.alias?.let { Text(it) }
                                    tag.boundAt?.let { Text("${stringResource(R.string.tag_bound_at)}: $it") }
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { vm.requestUnbind(tag) },
                                    modifier = Modifier,
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.tag_unbind))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── TagBindScreen (MH-TAG-01) ─────────────────────────────────────────────

/** MH-TAG-01: 绑定标签表单。tagCodePrefill 来自扫码结果（M3-C）。 */
@Composable
fun TagBindScreen(
    patientId: String,
    tagCodePrefill: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: TagBindViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(tagCodePrefill) {
        if (!tagCodePrefill.isNullOrBlank()) vm.prefill(tagCodePrefill)
    }
    LaunchedEffect(state.success) {
        if (state.success) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tag_bind_title)) },
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
                value = state.tagCode,
                onValueChange = vm::onTagCodeChange,
                label = { Text(stringResource(R.string.tag_field_tag_code)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.deviceType,
                onValueChange = vm::onDeviceTypeChange,
                label = { Text(stringResource(R.string.tag_field_device_type)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.alias,
                onValueChange = vm::onAliasChange,
                label = { Text(stringResource(R.string.tag_field_alias)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            state.error?.let {
                Text(ErrorMessageMapper.message(ctx, it), color = MaterialTheme.colorScheme.error)
            }
            MhPrimaryButton(
                text = stringResource(R.string.tag_bind_submit),
                contentDesc = stringResource(R.string.tag_bind_submit),
                onClick = { vm.submit(patientId) },
                enabled = !state.submitting,
            )
        }
    }
}
