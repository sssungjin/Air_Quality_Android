package com.monorama.airmonomatekr.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.AlarmManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.monorama.airmonomatekr.data.model.TransmissionMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleSensorDataWork(mode: TransmissionMode) {
        when (mode) {
            TransmissionMode.MINUTE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        // 사용자에게 정확한 알람 권한 요청
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return
                    }
                }

                val intent = Intent(context, SensorDataAlarmReceiver::class.java).apply {
                    action = "com.monorama.airmonomatekr.SENSOR_DATA_ALARM"
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Doze 모드에서도 동작하도록 설정
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Constants.Alarm.MINUTE_INTERVAL),
                    pendingIntent
                )
            }
            TransmissionMode.DAILY -> {
                val request = PeriodicWorkRequestBuilder<SensorDataWorker>(
                    Constants.Alarm.DAILY_INTERVAL, TimeUnit.HOURS
                ).setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).setInitialDelay(1, TimeUnit.MINUTES)  // 첫 실행을 위한 약간의 지연
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "sensor_data_work",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
            else -> println("Now mode is: $mode")
        }
    }

    fun cancelAllWork() {
        workManager.cancelUniqueWork("sensor_data_work")
        val intent = Intent(context, SensorDataAlarmReceiver::class.java).apply {
            action = "com.monorama.airmonomatekr.SENSOR_DATA_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}