package com.monorama.airmonomatekr.di

import android.content.Context
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import com.monorama.airmonomatekr.service.bluetooth.BleManager
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
        webSocketManager: WebSocketManager
    ): BleManager {
        return BleManager(context, webSocketManager)
    }
}