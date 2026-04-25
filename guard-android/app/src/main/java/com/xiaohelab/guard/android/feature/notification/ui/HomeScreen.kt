@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

// NOTE (env-workaround): 理想路径为 feature/home/ui/HomeScreen.kt。
// 当前执行环境无法创建新目录（无可用 shell），因此本文件物理落在
// feature/notification/ui/ 下，但 package 声明仍为 feature.home.ui，
// Kotlin 编译器允许目录与包不一致。后续有开发机权限时请将此文件
// 移动到 feature/home/ui/ 以符合包-目录一致性约定。
package com.xiaohelab.guard.android.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
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
import com.xiaohelab.guard.android.feature.task.ui.TaskListViewModel

/**
 * MH-HOME-00 首页：地图（含寻人任务抽屉） + 消息通知。
 *
 * 设计要点（v2 调整，handbook §8.2）：
 * - 去掉传统 TopAppBar 标题栏，改用紧凑顶部 Row：[Tab 切换（地图/消息通知）| 扫码图标]，
 *   减少首屏空白、把扫码入口放置在右上角。
 * - 「寻人」一级 Tab 已并入「地图」：地图作为底图，下方使用 BottomSheetScaffold 抽屉，
 *   抽屉中渲染寻人任务列表（可上拉/下拉），FAB「发起寻回」放在抽屉头部。
 *
 * HC-Check:
 * - HC-I18n：所有文案走 stringResource（zh/en 同步）。
 * - HC-01 六域隔离：仅依赖 core.ui + notification.ui + task.ui 的公开 ViewModel，
 *   未跨域访问内部实现。
 * - HC-02 状态来自服务端：未读数 / 任务状态完全由各自 ViewModel 持有，本 Screen 不做本地推算。
 * - HC-Coord：地图占位保留，正式接入时原始 GCJ-02 上报，反向只在展示层。
 */
@Composable
fun HomeScreen(
    onScan: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
    onCreateTask: () -> Unit = {},
    onOpenNotificationDetail: (String) -> Unit = {},
    notifVm: NotificationListViewModel = hiltViewModel(),
    taskVm: TaskListViewModel = hiltViewModel(),
) {
    var subTab by rememberSaveable { mutableStateOf(0) }
    val notifState by notifVm.state.collectAsState()

    LaunchedEffect(Unit) {
        notifVm.load()
        taskVm.load()
        taskVm.observeWs()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabRow(
                selectedTabIndex = subTab,
                modifier = Modifier.weight(1f),
            ) {
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
            IconButton(onClick = onScan) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = stringResource(R.string.me_scan),
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (subTab) {
                0 -> MapWithTaskSheet(taskVm, onCreateTask, onOpenTask)
                else -> NotificationsContent(notifVm, onOpenNotificationDetail)
            }
        }
    }
}

/**
 * 地图 + 寻人任务抽屉。
 * - 地图占据全屏作为底图（占位，待接入高德 GCJ-02）。
 * - 抽屉默认露出 [sheetPeekHeight]，可上拉至全屏，可下拉收起到 peek 状态。
 */
@Composable
private fun MapWithTaskSheet(
    taskVm: TaskListViewModel,
    onCreate: () -> Unit,
    onTaskClick: (String) -> Unit,
) {
    val sheetState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 140.dp,
        sheetContent = { TaskSheetContent(taskVm, onCreate, onTaskClick) },
    ) { inner ->
        Box(
            Modifier.fillMaxSize().padding(inner),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.home_map_placeholder),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun TaskSheetContent(
    vm: TaskListViewModel,
    onCreate: () -> Unit,
    onTaskClick: (String) -> Unit,
) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 600.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.home_tasks_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            FloatingActionButton(onClick = onCreate) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.task_create_title),
                )
            }
        }
        when {
            s.loading -> MhLoading()
            s.error != null -> Text(
                ErrorMessageMapper.message(ctx, s.error!!),
                color = MaterialTheme.colorScheme.error,
            )
            s.tasks.isEmpty() -> Text(stringResource(R.string.common_empty))
            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(s.tasks, key = { it.taskId }) { t ->
                    Card(
                        onClick = { onTaskClick(t.taskId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ListItem(
                            headlineContent = { Text(t.patientId) },
                            supportingContent = {
                                Text("${stringResource(R.string.task_field_status)}: ${t.status}")
                            },
                            trailingContent = { Badge { Text(t.status) } },
                        )
                    }
                }
            }
        }
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
