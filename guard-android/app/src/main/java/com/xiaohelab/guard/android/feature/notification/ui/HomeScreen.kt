@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

// NOTE (env-workaround): 理想路径为 feature/home/ui/HomeScreen.kt。
// 当前执行环境无法创建新目录（无可用 shell），因此本文件物理落在
// feature/notification/ui/ 下，但 package 声明仍为 feature.home.ui，
// Kotlin 编译器允许目录与包不一致。后续有开发机权限时请将此文件
// 移动到 feature/home/ui/ 以符合包-目录一致性约定。
package com.xiaohelab.guard.android.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.feature.notification.ui.NotificationListViewModel

/**
 * MH-HOME-00 首页：地图 + 消息通知（顶部 Tab 切换，handbook §8.2）。
 *
 * HC-Check:
 * - HC-I18n：所有文案走 stringResource（zh/en 同步）。
 * - HC-01 六域隔离：仅依赖 core.ui + notification.ui 的公开 ViewModel，
 *   未跨域访问 notification 内部实现。
 * - HC-02 状态来自服务端：未读数/列表完全由 NotificationListViewModel 持有，
 *   本 Screen 不做本地推算。
 * - HC-Coord：地图占位保留，正式接入时原始 GCJ-02 上报。
 */
@Composable
fun HomeScreen(
    notifVm: NotificationListViewModel = hiltViewModel(),
    onOpenNotificationDetail: (String) -> Unit = {},
) {
    var subTab by rememberSaveable { mutableStateOf(0) }
    val notifState by notifVm.state.collectAsState()

    LaunchedEffect(Unit) { notifVm.load() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_home)) }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = subTab) {
                Tab(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    text = { Text(stringResource(R.string.home_tab_map)) },
                )
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = {
                        BadgedBox(badge = {
                            if (notifState.unreadCount > 0) Badge { Text(notifState.unreadCount.toString()) }
                        }) { Text(stringResource(R.string.home_tab_notifications)) }
                    },
                )
            }
            Box(Modifier.fillMaxSize()) {
                when (subTab) {
                    0 -> MapPlaceholder()
                    else -> NotificationsContent(notifVm, onOpenNotificationDetail)
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.home_map_placeholder),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NotificationsContent(
    vm: NotificationListViewModel,
    onItemClick: (String) -> Unit,
) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    when {
        s.loading -> MhLoading()
        s.error != null -> Column(Modifier.padding(16.dp)) {
            Text(
                ErrorMessageMapper.message(ctx, s.error!!),
                color = MaterialTheme.colorScheme.error,
            )
        }
        s.notifications.isEmpty() -> Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.notification_empty))
        }
        else -> LazyColumn(contentPadding = PaddingValues(8.dp)) {
            items(s.notifications, key = { it.notificationId }) { n ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    onClick = {
                        if (!n.read) vm.onMarkRead(n.notificationId)
                        onItemClick(n.notificationId)
                    },
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                n.title,
                                style = if (!n.read)
                                    MaterialTheme.typography.titleMedium
                                else
                                    MaterialTheme.typography.bodyMedium,
                            )
                        },
                        supportingContent = { Text(n.body) },
                    )
                }
            }
        }
    }
}
