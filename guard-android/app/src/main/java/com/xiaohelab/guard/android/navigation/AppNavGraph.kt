package com.xiaohelab.guard.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xiaohelab.guard.android.feature.ai.screen.AiChatScreen
import com.xiaohelab.guard.android.feature.ai.screen.AiMemoryScreen
import com.xiaohelab.guard.android.feature.ai.screen.AiQuotaScreen
import com.xiaohelab.guard.android.feature.ai.screen.AiSessionListScreen
import com.xiaohelab.guard.android.feature.auth.screen.ChangePasswordScreen
import com.xiaohelab.guard.android.feature.auth.screen.LoginScreen
import com.xiaohelab.guard.android.feature.auth.screen.RegisterScreen
import com.xiaohelab.guard.android.feature.clue.screen.ClueDetailScreen
import com.xiaohelab.guard.android.feature.clue.screen.ManualEntryScreen
import com.xiaohelab.guard.android.feature.clue.screen.ReportClueScreen
import com.xiaohelab.guard.android.feature.clue.screen.ScanQrScreen
import com.xiaohelab.guard.android.feature.home.screen.HomeScreen
import com.xiaohelab.guard.android.feature.notification.screen.NotificationScreen
import com.xiaohelab.guard.android.feature.order.screen.CreateOrderScreen
import com.xiaohelab.guard.android.feature.order.screen.OrderDetailScreen
import com.xiaohelab.guard.android.feature.order.screen.OrderListScreen
import com.xiaohelab.guard.android.feature.patient.screen.FenceSettingScreen
import com.xiaohelab.guard.android.feature.patient.screen.GuardianScreen
import com.xiaohelab.guard.android.feature.patient.screen.PatientDetailScreen
import com.xiaohelab.guard.android.feature.patient.screen.PatientEditScreen
import com.xiaohelab.guard.android.feature.patient.screen.TagScreen
import com.xiaohelab.guard.android.feature.task.screen.CloseTaskScreen
import com.xiaohelab.guard.android.feature.task.screen.CreateTaskScreen
import com.xiaohelab.guard.android.feature.task.screen.TaskDetailScreen
import com.xiaohelab.guard.android.feature.task.screen.TaskListScreen
import com.xiaohelab.guard.android.feature.task.screen.TaskTrackScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // ─── Auth ──────────────────────────────────────────────────────────────
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Home ──────────────────────────────────────────────────────────────
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToTask = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) },
                onNavigateToNotifications = { navController.navigate(NavRoutes.NOTIFICATION) },
                onNavigateToPatient = { patientId -> navController.navigate(NavRoutes.patientDetail(patientId)) }
            )
        }

        // ─── Task ──────────────────────────────────────────────────────────────
        composable(NavRoutes.TASK_LIST) {
            TaskListScreen(
                onNavigateToDetail = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) },
                onNavigateToCreate = { navController.navigate(NavRoutes.CREATE_TASK) }
            )
        }

        composable(
            route = NavRoutes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClose = { taskId -> navController.navigate(NavRoutes.closeTask(taskId)) },
                onNavigateToTrack = { taskId -> navController.navigate(NavRoutes.taskTrack(taskId)) }
            )
        }

        composable(NavRoutes.CREATE_TASK) {
            CreateTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId)) {
                        popUpTo(NavRoutes.CREATE_TASK) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.CLOSE_TASK_ROUTE,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            CloseTaskScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.TASK_TRACK_ROUTE,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            TaskTrackScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Clue ──────────────────────────────────────────────────────────────
        composable(NavRoutes.SCAN_QR) {
            ScanQrScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReport = { _, taskId -> navController.navigate(NavRoutes.reportClue(taskId)) },
                onNavigateToManualEntry = { _ -> navController.navigate(NavRoutes.MANUAL_ENTRY) }
            )
        }

        composable(NavRoutes.MANUAL_ENTRY) {
            ManualEntryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReceipt = { clueId ->
                    navController.navigate(NavRoutes.clueDetail(clueId)) {
                        popUpTo(NavRoutes.SCAN_QR) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.REPORT_CLUE_ROUTE,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            ReportClueScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReceipt = { clueId ->
                    navController.navigate(NavRoutes.clueDetail(clueId)) {
                        popUpTo(NavRoutes.SCAN_QR) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.CLUE_DETAIL_ROUTE,
            arguments = listOf(navArgument("clueId") { type = NavType.StringType })
        ) {
            ClueDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Patient ───────────────────────────────────────────────────────────
        composable(
            route = NavRoutes.PATIENT_DETAIL_ROUTE,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            PatientDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { patientId -> navController.navigate(NavRoutes.patientEdit(patientId)) },
                onNavigateToFence = { patientId -> navController.navigate(NavRoutes.fenceSetting(patientId)) },
                onNavigateToGuardians = { patientId -> navController.navigate(NavRoutes.guardian(patientId)) },
                onNavigateToTag = { patientId -> navController.navigate(NavRoutes.tag(patientId)) }
            )
        }

        composable(NavRoutes.PATIENT_NEW) {
            PatientEditScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.PATIENT_EDIT_ROUTE,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            PatientEditScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.FENCE_SETTING_ROUTE,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            FenceSettingScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.GUARDIAN_ROUTE,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            GuardianScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.TAG_ROUTE,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) {
            TagScreen(onBack = { navController.popBackStack() })
        }

        // ─── Notification ──────────────────────────────────────────────────────
        composable(NavRoutes.NOTIFICATION) {
            NotificationScreen(onBack = { navController.popBackStack() })
        }

        // ─── AI ────────────────────────────────────────────────────────────────
        composable(NavRoutes.AI_SESSION_LIST) {
            AiSessionListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { sessionId -> navController.navigate(NavRoutes.aiChat(sessionId)) }
            )
        }

        composable(
            route = NavRoutes.AI_CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            AiChatScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.AI_QUOTA) {
            AiQuotaScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.AI_MEMORY) {
            AiMemoryScreen(onBack = { navController.popBackStack() })
        }

        // ─── Order ─────────────────────────────────────────────────────────────
        composable(NavRoutes.ORDER_LIST) {
            OrderListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { orderId -> navController.navigate(NavRoutes.orderDetail(orderId)) },
                onNavigateToCreate = { navController.navigate(NavRoutes.CREATE_ORDER) }
            )
        }

        composable(
            route = NavRoutes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) {
            OrderDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CREATE_ORDER) {
            CreateOrderScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
    }
}
