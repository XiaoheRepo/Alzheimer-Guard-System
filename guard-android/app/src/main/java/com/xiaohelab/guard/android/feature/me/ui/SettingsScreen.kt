package com.xiaohelab.guard.android.feature.me.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.i18n.LocaleManager
import com.xiaohelab.guard.android.core.storage.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
) : ViewModel() {
    val theme = settings.themeMode
    val language = settings.languageMode
    val largeText = settings.largeReadableMode

    fun setTheme(m: SettingsStore.ThemeMode) { viewModelScope.launch { settings.setThemeMode(m) } }
    fun setLanguage(m: SettingsStore.LanguageMode) { viewModelScope.launch {
        settings.setLanguageMode(m); LocaleManager.apply(m)
    } }
    fun setLargeText(enabled: Boolean) { viewModelScope.launch { settings.setLargeReadableMode(enabled) } }
}

/** MH-ME-02 设置页（HC-Theme / HC-I18n / HC-A11y）。 */
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val theme by vm.theme.collectAsState(initial = SettingsStore.ThemeMode.SYSTEM)
    val lang by vm.language.collectAsState(initial = SettingsStore.LanguageMode.SYSTEM)
    val large by vm.largeText.collectAsState(initial = false)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.settings_theme), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            SettingsStore.ThemeMode.values().forEach { mode ->
                ListItem(
                    headlineContent = { Text(when (mode) {
                        SettingsStore.ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        SettingsStore.ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        SettingsStore.ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    }) },
                    leadingContent = { RadioButton(selected = theme == mode, onClick = { vm.setTheme(mode) }) },
                    modifier = Modifier.selectable(selected = theme == mode, onClick = { vm.setTheme(mode) }),
                )
            }

            Text(stringResource(R.string.settings_language), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            SettingsStore.LanguageMode.values().forEach { mode ->
                ListItem(
                    headlineContent = { Text(when (mode) {
                        SettingsStore.LanguageMode.SYSTEM -> stringResource(R.string.settings_language_system)
                        SettingsStore.LanguageMode.ZH_CN -> stringResource(R.string.settings_language_zh)
                        SettingsStore.LanguageMode.EN_US -> stringResource(R.string.settings_language_en)
                    }) },
                    leadingContent = { RadioButton(selected = lang == mode, onClick = { vm.setLanguage(mode) }) },
                    modifier = Modifier.selectable(selected = lang == mode, onClick = { vm.setLanguage(mode) }),
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_large_text)) },
                supportingContent = { Text(stringResource(R.string.settings_large_text_hint)) },
                trailingContent = { Switch(checked = large, onCheckedChange = vm::setLargeText) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
