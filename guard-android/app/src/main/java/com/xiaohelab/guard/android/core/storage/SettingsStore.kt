package com.xiaohelab.guard.android.core.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "mh_settings")

/**
 * Non-sensitive user preferences (handbook §5.3 theme, §7 locale, §6 大字模式).
 * Token and PII go to [com.xiaohelab.guard.android.core.auth.AuthTokenStore] (HC-07).
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class ThemeMode { SYSTEM, LIGHT, DARK }
    enum class LanguageMode { SYSTEM, ZH_CN, EN_US }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { p ->
        ThemeMode.valueOf(p[K_THEME] ?: ThemeMode.SYSTEM.name)
    }
    val languageMode: Flow<LanguageMode> = context.settingsDataStore.data.map { p ->
        LanguageMode.valueOf(p[K_LANG] ?: LanguageMode.SYSTEM.name)
    }
    val largeReadableMode: Flow<Boolean> = context.settingsDataStore.data.map { p ->
        p[K_LARGE_TEXT] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[K_THEME] = mode.name }
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        context.settingsDataStore.edit { it[K_LANG] = mode.name }
    }

    suspend fun setLargeReadableMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[K_LARGE_TEXT] = enabled }
    }

    companion object {
        private val K_THEME: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        private val K_LANG: Preferences.Key<String> = stringPreferencesKey("language_mode")
        private val K_LARGE_TEXT: Preferences.Key<Boolean> = booleanPreferencesKey("large_text")
    }
}
