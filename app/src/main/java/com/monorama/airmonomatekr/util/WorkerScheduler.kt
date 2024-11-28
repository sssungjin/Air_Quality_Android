package com.monorama.airmonomatekr.util

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.TransmissionMode
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun scheduleSensorDataWork() {
        val settings = settingsDataStore.userSettings.first()

        // 기존 작업 취소
        workManager.cancelUniqueWork("sensor_data_work")

        when (settings.transmissionMode) {
            TransmissionMode.REALTIME -> {
                // 실시간 모드에서는 작업 스케줄링 하지 않음
            }
            TransmissionMode.MINUTE -> {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<SensorDataWorker>(
                    15, TimeUnit.MINUTES,
                    5, TimeUnit.MINUTES
                ).setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,  // MIN_BACKOFF_MILLIS 대신 DEFAULT_BACKOFF_MILLIS 사용
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                scheduleUniqueWork(request)
            }
            TransmissionMode.DAILY -> {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<SensorDataWorker>(
                    24, TimeUnit.HOURS
                ).setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                scheduleUniqueWork(request)
            }
        }
    }

    private fun scheduleUniqueWork(request: PeriodicWorkRequest) {
        workManager.enqueueUniquePeriodicWork(
            "sensor_data_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}