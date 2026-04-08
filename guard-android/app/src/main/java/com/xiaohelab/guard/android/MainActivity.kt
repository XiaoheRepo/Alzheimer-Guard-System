package com.xiaohelab.guard.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.xiaohelab.guard.android.core.datastore.TokenManager
import com.xiaohelab.guard.android.navigation.AppNavGraph
import com.xiaohelab.guard.android.navigation.NavRoutes
import com.xiaohelab.guard.android.ui.theme.GuardandroidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardandroidTheme {
                val navController = rememberNavController()
                val startDestination = if (tokenManager.isLoggedIn()) NavRoutes.HOME else NavRoutes.LOGIN
                AppNavGraph(navController = navController, startDestination = startDestination)
            }
        }
    }
}

