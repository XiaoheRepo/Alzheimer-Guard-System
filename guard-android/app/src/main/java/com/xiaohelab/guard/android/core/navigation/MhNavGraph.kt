package com.xiaohelab.guard.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xiaohelab.guard.android.feature.auth.ui.login.LoginScreen
import com.xiaohelab.guard.android.feature.auth.ui.register.RegisterScreen
import com.xiaohelab.guard.android.feature.auth.ui.reset.ResetConfirmScreen
import com.xiaohelab.guard.android.feature.auth.ui.reset.ResetRequestScreen
import com.xiaohelab.guard.android.feature.me.ui.MeScreen
import com.xiaohelab.guard.android.feature.me.ui.SettingsScreen
import com.xiaohelab.guard.android.feature.profile.ui.detail.PatientDetailScreen
import com.xiaohelab.guard.android.feature.profile.ui.edit.PatientEditScreen
import com.xiaohelab.guard.android.feature.profile.ui.guardian.GuardianInviteScreen
import com.xiaohelab.guard.android.feature.profile.ui.guardian.GuardianManageScreen
import com.xiaohelab.guard.android.feature.profile.ui.guardian.GuardianTransferScreen
import com.xiaohelab.guard.android.feature.profile.ui.list.PatientListScreen

/**
 * Compose Navigation graph — all IDs are STRINGs (HC-ID-String) and all page IDs
 * carry the `MH-*` prefix (handbook §1.4 / §16 / §17).
 */
@Composable
fun MhNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
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

        composable(
            MhRoutes.PATIENT_DETAIL,
            arguments = listOf(navArgument(MhRoutes.ARG_PATIENT_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(MhRoutes.ARG_PATIENT_ID).orEmpty()
            PatientDetailScreen(
                patientId = id,
                onEdit = { navController.navigate(MhRoutes.patientEdit(id)) },
                onGuardians = { navController.navigate(MhRoutes.guardianManage(id)) },
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
    }
}
