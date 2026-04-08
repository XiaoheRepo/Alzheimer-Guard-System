package com.xiaohelab.guard.android.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guard_settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}

/** 非敏感设置（通知开关、当前患者 ID 等轻量状态） */
@Singleton
class AppSettingsManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_CURRENT_PATIENT_ID = stringPreferencesKey("current_patient_id")
        val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val KEY_LAST_WS_RECONNECT_DELAY = intPreferencesKey("ws_reconnect_delay_ms")
    }

    val currentPatientId: Flow<String?> = dataStore.data.map { it[KEY_CURRENT_PATIENT_ID] }
    val notificationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIFICATION_ENABLED] ?: true }

    suspend fun setCurrentPatientId(patientId: String?) {
        dataStore.edit { prefs ->
            if (patientId == null) prefs.remove(KEY_CURRENT_PATIENT_ID)
            else prefs[KEY_CURRENT_PATIENT_ID] = patientId
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
