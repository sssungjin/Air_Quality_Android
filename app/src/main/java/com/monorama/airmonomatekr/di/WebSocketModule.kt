package com.monorama.airmonomatekr.di

import android.content.Context
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import com.monorama.airmonomatekr.util.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebSocketModule {
    @Provides
    @Singleton
    fun provideWebSocketManager(
        @ApplicationContext context: Context,
        settingsDataStore: SettingsDataStore,
        locationManager: LocationManager
    ): WebSocketManager {
        return WebSocketManager(
            context,
            settingsDataStore,
            locationManager
        )
    }
}