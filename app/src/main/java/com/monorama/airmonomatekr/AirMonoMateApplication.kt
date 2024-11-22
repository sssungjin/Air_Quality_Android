package com.monorama.airmonomatekr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AirMonoMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 전역 예외 처리기 추가
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            println("Fatal Exception in thread ${thread.name}")
            throwable.printStackTrace()
        }
    }
}