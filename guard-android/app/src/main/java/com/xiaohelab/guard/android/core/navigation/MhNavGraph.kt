package com.xiaohelab.guard.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xiaohelab.guard.android.core.scan.QrScanScreen
import com.xiaohelab.guard.android.feature.ai.ui.AiChatScreen
import com.xiaohelab.guard.android.feature.auth.ui.login.LoginScreen
import com.xiaohelab.guard.android.feature.auth.ui.register.RegisterScreen
import com.xiaohelab.guard.android.feature.auth.ui.reset.ResetConfirmScreen
import com.xiaohelab.guard.android.feature.auth.ui.reset.ResetRequestScreen
import com.xiaohelab.guard.android.feature.clue.ui.ClueCreateScreen
import com.xiaohelab.guard.android.feature.clue.ui.ClueListScreen
import com.xiaohelab.guard.android.feature.mat.ui.MaterialOrderCreateScreen
import com.xiaohelab.guard.android.feature.mat.ui.MaterialOrderListScreen
import com.xiaohelab.guard.android.feature.me.ui.MeScreen
import com.xiaohelab.guard.android.feature.me.ui.SettingsScreen
import com.xiaohelab.guard.android.feature.notification.ui.NotificationListScreen
import com.xiaohelab.guard.android.feature.profile.ui.fence.FenceEditScreen
import com.xiaohelab.guard.android.feature.profile.ui.detail.PatientDetailScreen
import com.xiaohelab.guard.android.feature.profile.ui.edit.PatientEditScreen
import com.xiaohelab.guard.android.feature.profile.ui.guardian.GuardianInviteScreen
import com.xiaohelab.guard.android.feature.profile.ui.guardian.GuardianManageScreen
import com.xiaohelab.guard.android.feature.profile.ui.guardian.GuardianTransferScreen
import com.xiaohelab.guard.android.feature.profile.ui.list.PatientListScreen
import com.xiaohelab.guard.android.feature.tag.ui.TagBindScreen
import com.xiaohelab.guard.android.feature.tag.ui.TagListScreen
import com.xiaohelab.guard.android.feature.task.ui.TaskCreateScreen
import com.xiaohelab.guard.android.feature.task.ui.TaskDetailScreen
import com.xiaohelab.guard.android.feature.task.ui.TaskListScreen

/**
 * Compose Navigation graph — all IDs are STRINGs (HC-ID-String) and all page IDs
 * carry the `MH-*` prefix (handbook §1.4 / §16 / §17).
 */
@Composable
fun MhNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        // ── Auth ─────────────────────────────────────────────────────────────
        composable(MhRoutes.AUTH_LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(MhRoutes.HOME) {
                        popUpTo(MhRoutes.AUTH_LOGIN) { inclusive = true }
                    }
                },
                onNavRegister = { navController.navigate(MhRoutes.AUTH_REGISTER) },
                onNavResetRequest = { navController.navigate(MhRoutes.AUTH_RESET_REQUEST) },
            )
        }
        composable(MhRoutes.AUTH_REGISTER) {
            RegisterScreen(onDone = { navController.popBackStack() })
        }
        composable(MhRoutes.AUTH_RESET_REQUEST) {
            ResetRequestScreen(
                onSubmitted = { navController.navigate(MhRoutes.AUTH_RESET_CONFIRM) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(MhRoutes.AUTH_RESET_CONFIRM) {
            ResetConfirmScreen(onDone = {
                navController.popBackStack(MhRoutes.AUTH_LOGIN, inclusive = false)
            })
        }

        // ── Home / Me ────────────────────────────────────────────────────────
        composable(MhRoutes.HOME) {
            PatientListScreen(
                onPatientClick = { id -> navController.navigate(MhRoutes.patientDetail(id)) },
                onCreate = { navController.navigate(MhRoutes.PATIENT_CREATE) },
                onMe = { navController.navigate(MhRoutes.ME) },
            )
        }
        composable(MhRoutes.ME) {
            MeScreen(
                onSettings = { navController.navigate(MhRoutes.SETTINGS) },
                onNotifications = { navController.navigate(MhRoutes.NOTIFICATION_LIST) },
                onAiChat = { navController.navigate(MhRoutes.aiChat()) },
                onLoggedOut = {
                    navController.navigate(MhRoutes.AUTH_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(MhRoutes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── Profile ──────────────────────────────────────────────────────────
        composable(
            MhRoutes.PATIENT_DETAIL,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            PatientDetailScreen(
                patientId = id,
                onEdit = { navController.navigate(MhRoutes.patientEdit(id)) },
                onGuardians = { navController.navigate(MhRoutes.guardianManage(id)) },
                onFenceEdit = { navController.navigate(MhRoutes.fenceEdit(id)) },
                onTagList = { navController.navigate(MhRoutes.tagList(id)) },
                onMatOrders = { navController.navigate(MhRoutes.matOrderList(id)) },
                onTasks = { navController.navigate(MhRoutes.TASK_LIST) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(MhRoutes.PATIENT_CREATE) {
            PatientEditScreen(patientId = null, onDone = { navController.popBackStack() })
        }
        composable(
            MhRoutes.PATIENT_EDIT,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID)
            PatientEditScreen(patientId = id, onDone = { navController.popBackStack() })
        }
        composable(
            MhRoutes.GUARDIAN_MANAGE,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            GuardianManageScreen(
                patientId = id,
                onInvite = { navController.navigate(MhRoutes.guardianInvite(id)) },
                onTransfer = { navController.navigate(MhRoutes.guardianTransfer(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            MhRoutes.GUARDIAN_INVITE,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            GuardianInviteScreen(patientId = id, onDone = { navController.popBackStack() })
        }
        composable(
            MhRoutes.GUARDIAN_TRANSFER,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            GuardianTransferScreen(patientId = id, onDone = { navController.popBackStack() })
        }

        // ── M4: Fence edit ───────────────────────────────────────────────────
        composable(
            MhRoutes.FENCE_EDIT,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            FenceEditScreen(
                patientId = id,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        // ── M3-A: Tags ───────────────────────────────────────────────────────
        composable(
            MhRoutes.TAG_LIST,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            TagListScreen(
                patientId = id,
                onBind = { navController.navigate(MhRoutes.tagBind(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            MhRoutes.TAG_BIND,
            arguments = listOf(
                navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType },
                navArgument(MhRoutes.ARG_TAG_CODE) { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val patientId = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            val tagCode = entry.arguments?.getString(MhRoutes.ARG_TAG_CODE).orEmpty().ifEmpty { null }
            TagBindScreen(
                patientId = patientId,
                tagCodePrefill = tagCode,
                onScan = { navController.navigate(MhRoutes.qrScan("tag_bind:$patientId")) },
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        // ── M3-B: Material orders ────────────────────────────────────────────
        composable(
            MhRoutes.MAT_ORDER_LIST,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            MaterialOrderListScreen(
                patientId = id,
                onCreate = { navController.navigate(MhRoutes.matOrderCreate(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            MhRoutes.MAT_ORDER_CREATE,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            MaterialOrderCreateScreen(
                patientId = id,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        // ── M3-C: QR Scan ────────────────────────────────────────────────────
        composable(
            MhRoutes.QR_SCAN,
            arguments = listOf(
                navArgument(MhRoutes.ARG_TARGET) { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val target = entry.arguments?.getString(MhRoutes.ARG_TARGET).orEmpty()
            QrScanScreen(
                onScanned = { tagCode ->
                    // 扫码成功：回到 tagBind 页并带入 tag_code prefill
                    // target 携带 patient_id，格式 "tag_bind:<patient_id>"
                    val patientId = target.removePrefix("tag_bind:")
                    navController.navigate(MhRoutes.tagBind(patientId, tagCode)) {
                        popUpTo(MhRoutes.QR_SCAN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        // ── M5-A: Tasks ──────────────────────────────────────────────────────
        composable(MhRoutes.TASK_LIST) {
            TaskListScreen(
                onTaskClick = { taskId -> navController.navigate(MhRoutes.taskDetail(taskId)) },
                onCreate = { navController.navigate(MhRoutes.TASK_CREATE) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(MhRoutes.TASK_CREATE) {
            TaskCreateScreen(
                onCreated = { taskId ->
                    navController.navigate(MhRoutes.taskDetail(taskId)) {
                        popUpTo(MhRoutes.TASK_CREATE) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            MhRoutes.TASK_DETAIL,
            arguments = listOf(navArgument(MhRoutes.ARG_TASK_ID) { type = NavType.StringType }),
        ) { entry ->
            val taskId = entry.arguments?.getString(MhRoutes.ARG_TASK_ID).orEmpty()
            TaskDetailScreen(
                taskId = taskId,
                onClues = { navController.navigate(MhRoutes.clueList(taskId)) },
                onBack = { navController.popBackStack() },
            )
        }

        // ── M5-B: Clues ──────────────────────────────────────────────────────
        composable(
            MhRoutes.CLUE_LIST,
            arguments = listOf(navArgument(MhRoutes.ARG_TASK_ID) { type = NavType.StringType }),
        ) { entry ->
            val taskId = entry.arguments?.getString(MhRoutes.ARG_TASK_ID).orEmpty()
            ClueListScreen(
                taskId = taskId,
                onCreate = { navController.navigate(MhRoutes.clueCreate(taskId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            MhRoutes.CLUE_CREATE,
            arguments = listOf(navArgument(MhRoutes.ARG_TASK_ID) { type = NavType.StringType }),
        ) { entry ->
            val taskId = entry.arguments?.getString(MhRoutes.ARG_TASK_ID).orEmpty()
            ClueCreateScreen(
                taskId = taskId,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        // ── M6: Notifications ────────────────────────────────────────────────
        composable(MhRoutes.NOTIFICATION_LIST) {
            NotificationListScreen(onBack = { navController.popBackStack() })
        }

        // ── M7: AI Chat ──────────────────────────────────────────────────────
        composable(
            MhRoutes.AI_CHAT,
            arguments = listOf(
                navArgument(MhRoutes.ARG_SESSION_ID) { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val sessionId = entry.arguments?.getString(MhRoutes.ARG_SESSION_ID).orEmpty().ifEmpty { null }
            AiChatScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
