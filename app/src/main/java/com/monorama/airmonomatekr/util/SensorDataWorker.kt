package com.monorama.airmonomatekr.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Date

@HiltWorker
class SensorDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sensorLogManager: SensorLogManager,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        println("SensorDataWorker: Starting work at ${Date(startTime)}")

        return try {
            // 현재 모드 확인
            val currentMode = settingsDataStore.getTransmissionMode().first()
            println("SensorDataWorker: Current transmission mode: $currentMode")

            sensorLogManager.uploadPendingLogs()

            val endTime = System.currentTimeMillis()
            println("SensorDataWorker: Work completed successfully at ${Date(endTime)}")
            println("SensorDataWorker: Execution took ${endTime - startTime}ms")

            Result.success()
        } catch (e: Exception) {
            println("SensorDataWorker: Work failed - ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }
}