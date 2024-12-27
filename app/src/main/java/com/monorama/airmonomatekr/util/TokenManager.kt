package com.monorama.airmonomatekr.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenManager(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "token_store")

    private object PreferencesKeys {
        val TOKEN = stringPreferencesKey("jwt_token")
    }

    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOKEN]
    }

    suspend fun getToken(): String? {
        return token.first()
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOKEN] = token
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.TOKEN)
        }
    }
}