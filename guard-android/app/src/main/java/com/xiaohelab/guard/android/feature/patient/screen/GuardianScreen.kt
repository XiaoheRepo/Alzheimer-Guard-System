package com.xiaohelab.guard.android.feature.patient.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.Guardian
import com.xiaohelab.guard.android.domain.model.GuardianStatus
import com.xiaohelab.guard.android.feature.patient.GuardianUiEffect
import com.xiaohelab.guard.android.feature.patient.GuardianViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianScreen(
    onBack: () -> Unit,
    viewModel: GuardianViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showInviteSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GuardianUiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("监护人管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showInviteSheet = true }) {
                Text("邀请监护人")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            items(state.guardians, key = { it.id }) { guardian ->
                GuardianCard(
                    guardian = guardian,
                    onRemove = { viewModel.removeGuardian(guardian.id) },
                    onTransfer = { showTransferDialog = guardian.id }
                )
            }
        }
    }

    if (showInviteSheet) {
        ModalBottomSheet(onDismissRequest = { showInviteSheet = false }) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("邀请监护人", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.invitePhone, onValueChange = viewModel::onInvitePhoneChange,
                    label = { Text("手机号") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.inviteRelation, onValueChange = viewModel::onInviteRelationChange,
                    label = { Text("关系（如：子女）") }, modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.inviteGuardian(); showInviteSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("发送邀请") }
            }
        }
    }

    showTransferDialog?.let { guardianId ->
        AlertDialog(
            onDismissRequest = { showTransferDialog = null },
            title = { Text("移交主监护人") },
            text = { Text("确定要将主监护权移交给此用户？移交后您将成为普通监护人。") },
            confirmButton = {
                TextButton(onClick = { viewModel.transferOwner(guardianId); showTransferDialog = null }) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun GuardianCard(guardian: Guardian, onRemove: () -> Unit, onTransfer: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(guardian.username, style = MaterialTheme.typography.bodyLarge)
                guardian.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                guardian.relation?.let { Text("关系：$it", style = MaterialTheme.typography.bodySmall) }
                Text(
                    when (guardian.status) {
                        GuardianStatus.ACTIVE -> "已激活"
                        GuardianStatus.PENDING -> "待接受"
                        GuardianStatus.UNKNOWN -> "未知"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Row {
                if (guardian.role != "OWNER") {
                    TextButton(onClick = onTransfer) { Text("移交") }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "移除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
