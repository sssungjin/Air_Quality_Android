package com.monorama.airmonomatekr.di

import android.content.Context
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import com.monorama.airmonomatekr.service.bluetooth.BleManager
import com.monorama.airmonomatekr.util.SensorLogManager
import com.monorama.airmonomatekr.util.WorkerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBleManager(
        @ApplicationContext context: Context,
        webSocketManager: WebSocketManager,
        settingsDataStore: SettingsDataStore,
        sensorLogManager: SensorLogManager,
        workerScheduler: WorkerScheduler
    ): BleManager {
        return BleManager(
            context = context,
            webSocketManager = webSocketManager,
            settingsDataStore = settingsDataStore,
            sensorLogManager = sensorLogManager,
            workerScheduler = workerScheduler
        )
    }
}