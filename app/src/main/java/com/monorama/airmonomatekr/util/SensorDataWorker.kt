package com.monorama.airmonomatekr.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first

@HiltWorker
class SensorDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sensorLogManager: SensorLogManager,
    private val settingsDataStore: SettingsDataStore,
    private val webSocketManager: WebSocketManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsDataStore.userSettings.first()

        return when (settings.transmissionMode) {
            TransmissionMode.REALTIME -> {
                // 실시간 모드에서는 아무것도 하지 않음 (WebSocket이 처리)
                Result.success()
            }
            TransmissionMode.MINUTE -> {
                // 1분마다 실행되는 작업
                try {
                    sensorLogManager.uploadPendingLogs()
                    Result.success()
                } catch (e: Exception) {
                    Result.retry()
                }
            }
            TransmissionMode.DAILY -> {
                // 매일 실행되는 작업
                try {
                    sensorLogManager.uploadPendingLogs()
                    Result.success()
                } catch (e: Exception) {
                    Result.retry()
                }
            }
        }
    }
}