package com.xiaohelab.guard.android.feature.profile.ui.guardian

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.profile.domain.InitiatePrimaryTransferUseCase
import com.xiaohelab.guard.android.feature.profile.domain.InviteGuardianUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------- MH-GUA-01 Manage ----------
@Composable
fun GuardianManageScreen(
    patientId: String,
    onInvite: () -> Unit,
    onTransfer: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.guardian_manage)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.guardian_manage_hint))
            MhPrimaryButton(
                text = stringResource(R.string.guardian_invite),
                contentDesc = stringResource(R.string.guardian_invite),
                onClick = onInvite,
                modifier = Modifier.fillMaxWidth(),
            )
            MhPrimaryButton(
                text = stringResource(R.string.guardian_transfer),
                contentDesc = stringResource(R.string.guardian_transfer),
                onClick = onTransfer,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- MH-GUA-02 Invite ----------

data class InviteUiState(
    val identifier: String = "",
    val relationship: String = "",
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class GuardianInviteViewModel @Inject constructor(private val useCase: InviteGuardianUseCase) : ViewModel() {
    private val _s = MutableStateFlow(InviteUiState())
    val state: StateFlow<InviteUiState> = _s.asStateFlow()
    fun onIdentifier(v: String) = _s.update { it.copy(identifier = v, error = null) }
    fun onRelationship(v: String) = _s.update { it.copy(relationship = v, error = null) }
    fun submit(patientId: String) {
        val s = _s.value
        if (s.loading || s.identifier.isBlank()) return
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = useCase(patientId, s.identifier, s.relationship.ifBlank { null })) {
                is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

@Composable
fun GuardianInviteScreen(
    patientId: String,
    onDone: () -> Unit,
    vm: GuardianInviteViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(state.success) { if (state.success) onDone() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.guardian_invite_title)) },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(state.identifier, vm::onIdentifier, label = { Text(stringResource(R.string.guardian_field_identifier)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.relationship, vm::onRelationship, label = { Text(stringResource(R.string.guardian_field_relationship)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            state.error?.let { e -> Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            MhPrimaryButton(
                text = stringResource(R.string.guardian_invite_submit),
                contentDesc = stringResource(R.string.guardian_invite_submit),
                onClick = { vm.submit(patientId) },
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading,
            )
        }
    }
}

// ---------- MH-GUA-04 Primary Transfer (FLAG_SECURE) ----------

data class TransferUiState(
    val targetUserId: String = "",
    val reason: String = "",
    val version: Long = 0,
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class GuardianTransferViewModel @Inject constructor(private val useCase: InitiatePrimaryTransferUseCase) : ViewModel() {
    private val _s = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _s.asStateFlow()
    fun onTarget(v: String) = _s.update { it.copy(targetUserId = v, error = null) }
    fun onReason(v: String) = _s.update { it.copy(reason = v, error = null) }
    fun onVersion(v: Long) = _s.update { it.copy(version = v) }
    fun submit(patientId: String) {
        val s = _s.value
        if (s.loading || s.targetUserId.isBlank()) return
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = useCase(patientId, s.targetUserId, s.reason.ifBlank { null }, s.version)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

@Composable
fun GuardianTransferScreen(
    patientId: String,
    onDone: () -> Unit,
    vm: GuardianTransferViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val activity = ctx as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(state.success) { if (state.success) onDone() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.guardian_transfer_title)) },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.guardian_transfer_warn), color = MaterialTheme.colorScheme.error)
            OutlinedTextField(state.targetUserId, vm::onTarget, label = { Text(stringResource(R.string.guardian_field_target_user)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.reason, vm::onReason, label = { Text(stringResource(R.string.guardian_field_reason)) }, modifier = Modifier.fillMaxWidth())
            state.error?.let { e -> Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            MhPrimaryButton(
                text = stringResource(R.string.guardian_transfer_submit),
                contentDesc = stringResource(R.string.guardian_transfer_submit),
                onClick = { vm.submit(patientId) },
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading,
            )
        }
    }
}
