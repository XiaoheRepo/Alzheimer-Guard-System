@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.feature.ai.ui.AiSessionsListScreen
import com.xiaohelab.guard.android.feature.home.ui.HomeScreen
import com.xiaohelab.guard.android.feature.me.ui.MeScreen
import com.xiaohelab.guard.android.feature.profile.ui.list.PatientListScreen
import com.xiaohelab.guard.android.feature.task.ui.TaskListScreen

/** 底部导航 5 Tab（首页 / 档案 / 寻人 / AI / 我的）。首页内嵌 Map + 通知顶部切换。 */
private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
    Home(R.string.nav_home, Icons.Filled.Home),
    Profile(R.string.nav_profile, Icons.Filled.Groups),
    Tasks(R.string.nav_tasks, Icons.Filled.Search),
    Ai(R.string.nav_ai, Icons.Filled.SmartToy),
    Me(R.string.nav_me, Icons.Filled.Person),
}

/**
 * MH-MAIN：登录后的主壳。五个一级入口承载家属端高频功能。
 * - 首页 = 地图 + 消息通知（顶部 Tab 切换，handbook §8.2）。
 */
@Composable
fun MainScaffold(navController: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = stringResource(t.labelRes)) },
                        label = { Text(stringResource(t.labelRes)) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                MainTab.Home -> HomeScreen()
                MainTab.Profile -> PatientListScreen(
                    onPatientClick = { id -> navController.navigate(MhRoutes.patientDetail(id)) },
                    onCreate = { navController.navigate(MhRoutes.PATIENT_CREATE) },
                    onMe = { tab = MainTab.Me },
                )
                MainTab.Tasks -> TaskListScreen(
                    onTaskClick = { taskId -> navController.navigate(MhRoutes.taskDetail(taskId)) },
                    onCreate = { navController.navigate(MhRoutes.TASK_CREATE) },
                    onBack = { /* root tab — no back */ },
                )
                MainTab.Ai -> AiSessionsListScreen(
                    onOpenSession = { sessionId -> navController.navigate(MhRoutes.aiChat(sessionId)) },
                )
                MainTab.Me -> MeScreen(
                    onSettings = { navController.navigate(MhRoutes.SETTINGS) },
                    onNotifications = { navController.navigate(MhRoutes.NOTIFICATION_LIST) },
                    onAiChat = { tab = MainTab.Ai },
                    onChangePassword = { navController.navigate(MhRoutes.ME_CHANGE_PASSWORD) },
                    onLoggedOut = {
                        navController.navigate(MhRoutes.AUTH_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { tab = MainTab.Profile },
                )
            }
        }
    }
}
