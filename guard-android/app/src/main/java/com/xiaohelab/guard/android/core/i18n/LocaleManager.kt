package com.xiaohelab.guard.android.core.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.xiaohelab.guard.android.core.storage.SettingsStore

/**
 * HC-I18n: `AppCompatDelegate.setApplicationLocales` switch without restarting Activity.
 * Default `zh-CN`; handbook §7.
 */
object LocaleManager {
    fun apply(mode: SettingsStore.LanguageMode) {
        val tags = when (mode) {
            SettingsStore.LanguageMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            SettingsStore.LanguageMode.ZH_CN -> LocaleListCompat.forLanguageTags("zh-CN")
            SettingsStore.LanguageMode.EN_US -> LocaleListCompat.forLanguageTags("en-US")
        }
        AppCompatDelegate.setApplicationLocales(tags)
    }
}
