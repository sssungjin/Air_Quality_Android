package com.monorama.airmonomatekr.data.local

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.monorama.airmonomatekr.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.monorama.airmonomatekr.data.model.TransmissionMode

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private object PreferencesKeys {
        val USER_ID = longPreferencesKey("user_id")
        val PROJECT_ID = stringPreferencesKey("project_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("email")
        val TRANSMISSION_MODE = stringPreferencesKey("transmission_mode")
        val UPLOAD_INTERVAL = stringPreferencesKey("upload_interval")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            userId = preferences[PreferencesKeys.USER_ID] ?: 0,
            projectId = preferences[PreferencesKeys.PROJECT_ID] ?: "",
            userName = preferences[PreferencesKeys.USER_NAME] ?: "",
            email = preferences[PreferencesKeys.EMAIL] ?: "",
            transmissionMode = TransmissionMode.valueOf(
                preferences[PreferencesKeys.TRANSMISSION_MODE] ?: TransmissionMode.REALTIME.name
            ),
            uploadInterval = preferences[PreferencesKeys.UPLOAD_INTERVAL]?.toInt() ?: 5
        )
    }

    fun getTransmissionMode(): Flow<TransmissionMode> = context.dataStore.data.map { preferences ->
        TransmissionMode.valueOf(
            preferences[PreferencesKeys.TRANSMISSION_MODE] ?: TransmissionMode.REALTIME.name
        )
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = settings.userId
            preferences[PreferencesKeys.PROJECT_ID] = settings.projectId
            preferences[PreferencesKeys.USER_NAME] = settings.userName
            preferences[PreferencesKeys.EMAIL] = settings.email
            preferences[PreferencesKeys.TRANSMISSION_MODE] = settings.transmissionMode.name
            preferences[PreferencesKeys.UPLOAD_INTERVAL] = settings.uploadInterval.toString()
        }
    }
} 