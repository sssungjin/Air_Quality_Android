package com.monorama.airmonomatekr.di

import android.content.Context
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
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
        @ApplicationContext context: Context
    ): WebSocketManager {
        return WebSocketManager(context)
    }
}