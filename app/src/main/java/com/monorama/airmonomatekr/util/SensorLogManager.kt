package com.monorama.airmonomatekr.util

import android.content.Context
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.SensorDataLogDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// SensorLogManager.kt
@Singleton
class SensorLogManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val logFolder = File(context.filesDir, "sensor_logs")

    init {
        logFolder.mkdirs()
    }

    fun writeLog(sensorData: SensorLogData, deviceId: String, projectId: Long) {
        val today = dateFormat.format(Date())
        val logFile = File(logFolder, "${today}_${deviceId}.csv")

        if (!logFile.exists()) {
            // 헤더 작성
            logFile.writeText("timestamp,deviceId,projectId,pm25Value,pm25Level,pm10Value,pm10Level," +
                    "temperature,temperatureLevel,humidity,humidityLevel,co2Value,co2Level,vocValue,vocLevel\n")
        }

        // 데이터 작성
        logFile.appendText("${sensorData.timestamp},${deviceId},${projectId}," +
                "${sensorData.pm25.value},${sensorData.pm25.level}," +
                "${sensorData.pm10.value},${sensorData.pm10.level}," +
                "${sensorData.temperature.value},${sensorData.temperature.level}," +
                "${sensorData.humidity.value},${sensorData.humidity.level}," +
                "${sensorData.co2.value},${sensorData.co2.level}," +
                "${sensorData.voc.value},${sensorData.voc.level}\n")
    }

    suspend fun uploadPendingLogs() {
        logFolder.listFiles()?.forEach { file ->
            if (!file.name.startsWith(dateFormat.format(Date()))) {
                try {
                    val logs = parseCsvFile(file)
                    if (logs.isNotEmpty()) {
                        val response = apiService.uploadSensorLogs(logs)
                        if (response.success) {
                            file.delete()
                            println("Successfully uploaded and deleted log file: ${file.name}, processed count: ${response.processedCount}")
                        } else {
                            println("Failed to upload log file: ${file.name}, server response: ${response.message}")
                        }
                    }
                } catch (e: Exception) {
                    println("Failed to upload log file: ${file.name}, error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }


    private fun parseCsvFile(file: File): List<SensorDataLogDto> {
        return try {
            file.readLines()
                .drop(1)  // 헤더 제외
                .mapNotNull { line ->
                    try {
                        val values = line.split(",")
                        SensorDataLogDto(
                            deviceId = values[1],
                            projectId = values[2].toLong(),
                            timestamp = Instant.ofEpochMilli(values[0].toLong()).toString(),
                            pm25Value = values[3].toDouble(),
                            pm25Level = values[4].toInt(),
                            pm10Value = values[5].toDouble(),
                            pm10Level = values[6].toInt(),
                            temperature = values[7].toDouble(),
                            temperatureLevel = values[8].toInt(),
                            humidity = values[9].toDouble(),
                            humidityLevel = values[10].toInt(),
                            co2Value = values[11].toDouble(),
                            co2Level = values[12].toInt(),
                            vocValue = values[13].toDouble(),
                            vocLevel = values[14].toInt(),
                            latitude = values.getOrNull(15)?.toDoubleOrNull(),
                            longitude = values.getOrNull(16)?.toDoubleOrNull()
                        )
                    } catch (e: Exception) {
                        println("Error parsing line: $line")
                        null
                    }
                }
        } catch (e: Exception) {
            println("Error reading file: ${file.name}")
            emptyList()
        }
    }
}
