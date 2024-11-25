package com.monorama.airmonomatekr.data.local

import android.content.Context
import com.monorama.airmonomatekr.data.model.SensorLogData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import android.provider.Settings


@Singleton
class LogFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val baseDir = context.getExternalFilesDir(null)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun writeLog(data: SensorLogData, projectId: String) {
        val today = dateFormat.format(Date())
        val logFile = File(baseDir, "sensor_log_${today}.csv")
        
        if (!logFile.exists()) {
            // 헤더 추가
            logFile.writeText("timestamp,project_id,device_id,pm25,pm25_level,pm10,pm10_level,temperature,temperature_level,humidity,humidity_level,co2,co2_level,voc,voc_level,latitude,longitude\n")
        }

        val logLine = buildString {
            append(data.timestamp).append(",")
            append(projectId).append(",")
            append(getDeviceId()).append(",")
            append(data.pm25.value).append(",")
            append(data.pm25.level).append(",")
            // ... 나머지 데이터 추가
            append("\n")
        }

        logFile.appendText(logLine)
    }

    fun getOldLogFiles(): List<File> {
        val today = dateFormat.format(Date())
        return baseDir?.listFiles { file ->
            file.name.startsWith("sensor_log_") && 
            file.name.endsWith(".csv") && 
            !file.name.contains(today)
        }?.toList() ?: emptyList()
    }

    fun deleteLogFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
} 