@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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

/** 底部导航 4 Tab（首页 / 档案 / AI / 我的）。寻人任务作为首页地图抽屉，不再独立 Tab。 */
private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
    Home(R.string.nav_home, Icons.Filled.Home),
    Profile(R.string.nav_profile, Icons.Filled.Groups),
    Ai(R.string.nav_ai, Icons.Filled.SmartToy),
    Me(R.string.nav_me, Icons.Filled.Person),
}

/**
 * MH-MAIN：登录后的主壳。四个一级入口承载家属端高频功能。
 * - 首页 = 地图（含寻人任务抽屉）+ 消息通知（顶部 Tab 切换，扫码入口右上角）。
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
                MainTab.Home -> HomeScreen(
                    onScan = { navController.navigate(MhRoutes.qrScan(MhRoutes.SCAN_TARGET_ME_ENTRY)) },
                    onOpenTask = { id -> navController.navigate(MhRoutes.taskDetail(id)) },
                    onCreateTask = { navController.navigate(MhRoutes.TASK_CREATE) },
                    onOpenNotificationDetail = { /* 通知列表项已在 ViewModel 内 markRead；详情页待 B7 接入 */ },
                )
                MainTab.Profile -> PatientListScreen(
                    onPatientClick = { id -> navController.navigate(MhRoutes.patientDetail(id)) },
                    onCreate = { navController.navigate(MhRoutes.PATIENT_CREATE) },
                    onMe = { tab = MainTab.Me },
                )
                MainTab.Ai -> AiSessionsListScreen(
                    onOpenSession = { sessionId -> navController.navigate(MhRoutes.aiChat(sessionId)) },
                )
                MainTab.Me -> MeScreen(
                    onSettings = { navController.navigate(MhRoutes.SETTINGS) },
                    onScan = { navController.navigate(MhRoutes.qrScan(MhRoutes.SCAN_TARGET_ME_ENTRY)) },
                    onChangePassword = { navController.navigate(MhRoutes.ME_CHANGE_PASSWORD) },
                    onLoggedOut = {
                        navController.navigate(MhRoutes.AUTH_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
