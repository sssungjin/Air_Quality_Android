package com.monorama.airmonomatekr.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SensorDataAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.monorama.airmonomatekr.SENSOR_DATA_ALARM") {
            val request = OneTimeWorkRequestBuilder<SensorDataWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)

            // 다음 알람 설정
            val nextIntent = Intent(context, SensorDataAlarmReceiver::class.java).apply {
                action = "com.monorama.airmonomatekr.SENSOR_DATA_ALARM"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Constants.Alarm.MINUTE_INTERVAL),
                pendingIntent
            )
        }
    }
}