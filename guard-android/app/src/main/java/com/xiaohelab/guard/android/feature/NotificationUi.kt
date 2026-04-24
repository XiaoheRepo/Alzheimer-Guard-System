@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.notification.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.xiaohelab.guard.android.core.eventbus.AppEventBus
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.feature.notification.data.NotificationDto
import com.xiaohelab.guard.android.feature.notification.domain.ListNotificationsUseCase
import com.xiaohelab.guard.android.feature.notification.domain.MarkAllNotificationsReadUseCase
import com.xiaohelab.guard.android.feature.notification.domain.MarkNotificationReadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── NotificationListViewModel ────────────────────────────────────────────────

data class NotificationListUiState(
    val loading: Boolean = true,
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val error: DomainException? = null,
)

@HiltViewModel
class NotificationListViewModel @Inject constructor(
    private val listNotifications: ListNotificationsUseCase,
    private val markRead: MarkNotificationReadUseCase,
    private val markAllRead: MarkAllNotificationsReadUseCase,
    private val eventBus: AppEventBus,
) : ViewModel() {
    private val _s = MutableStateFlow(NotificationListUiState())
    val state: StateFlow<NotificationListUiState> = _s.asStateFlow()

    fun load() {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = listNotifications()) {
                is MhResult.Success -> {
                    _s.update {
                        it.copy(
                            loading = false,
                            notifications = r.data.items,
                            unreadCount = r.data.unreadCount,
                        )
                    }
                    // 广播未读数变化
                    eventBus.tryEmit(
                        com.xiaohelab.guard.android.core.eventbus.AppEvent.NotificationUnreadCountChanged(r.data.unreadCount)
                    )
                }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    fun onMarkRead(notificationId: String) {
        viewModelScope.launch {
            markRead(notificationId)
            load()
        }
    }

    fun onMarkAllRead() {
        viewModelScope.launch {
            markAllRead()
            load()
        }
    }
}

// ─── NotificationListScreen (MH-NOTIF-00) ────────────────────────────────────

/** MH-NOTIF-00: 通知中心。轮询由 NotificationSyncWorker 完成（HC-05 间隔来自远端配置）。 */
@Composable
fun NotificationListScreen(
    onBack: () -> Unit,
    vm: NotificationListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${stringResource(R.string.notification_list_title)}${if (state.unreadCount > 0) " (${state.unreadCount})" else ""}"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = vm::onMarkAllRead) {
                        Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.notification_read_all))
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
            state.notifications.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                Text(stringResource(R.string.notification_empty))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.padding(pad),
            ) {
                items(state.notifications, key = { it.notificationId }) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        onClick = { if (!notif.read) vm.onMarkRead(notif.notificationId) },
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    notif.title,
                                    style = if (!notif.read)
                                        MaterialTheme.typography.titleMedium
                                    else
                                        MaterialTheme.typography.bodyMedium,
                                )
                            },
                            supportingContent = { Text(notif.body) },
                            overlineContent = { if (!notif.read) Text(stringResource(R.string.notification_mark_read)) },
                        )
                    }
                }
            }
        }
    }
}
