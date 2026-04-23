package com.xiaohelab.guard.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.xiaohelab.guard.android.core.auth.AuthTokenStore
import com.xiaohelab.guard.android.core.navigation.MhNavGraph
import com.xiaohelab.guard.android.core.navigation.MhRoutes
import com.xiaohelab.guard.android.core.storage.SettingsStore
import com.xiaohelab.guard.android.core.theme.MhTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var tokenStore: AuthTokenStore
    @Inject lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = SettingsStore.ThemeMode.SYSTEM)
            val largeText by settings.largeReadableMode.collectAsState(initial = false)
            val darkTheme = when (themeMode) {
                SettingsStore.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                SettingsStore.ThemeMode.LIGHT -> false
                SettingsStore.ThemeMode.DARK -> true
            }
            MhTheme(darkTheme = darkTheme, accessibilityLarge = largeText) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    val start = if (tokenStore.hasSession()) MhRoutes.HOME else MhRoutes.AUTH_LOGIN
                    MhNavGraph(navController = nav, startDestination = start)
                }
            }
        }
    }
}


