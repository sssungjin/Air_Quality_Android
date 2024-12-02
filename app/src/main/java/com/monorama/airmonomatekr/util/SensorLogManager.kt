package com.monorama.airmonomatekr.util

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.SensorDataLogDto
import com.monorama.airmonomatekr.network.api.dto.SensorDataPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

// SensorLogManager.kt
@Singleton
class SensorLogManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val objectMapper: ObjectMapper
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // Asia/Seoul 시간대로 ISO 8601 형식 포매터 설정
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }
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

        // timestamp를 서울 시간대 기준 ISO 8601 형식으로 포맷팅
        val formattedTimestamp = timestampFormat.format(Date(sensorData.timestamp))

        // 데이터 작성
        logFile.appendText("${formattedTimestamp},${deviceId},${projectId}," +
                "${sensorData.pm25.value},${sensorData.pm25.level}," +
                "${sensorData.pm10.value},${sensorData.pm10.level}," +
                "${sensorData.temperature.value},${sensorData.temperature.level}," +
                "${sensorData.humidity.value},${sensorData.humidity.level}," +
                "${sensorData.co2.value},${sensorData.co2.level}," +
                "${sensorData.voc.value},${sensorData.voc.level}\n")
    }

    private fun convertToJsonFormat(logs: List<SensorDataLogDto>): String {
        val payload = logs.map { log ->
            mapOf(
                "deviceId" to log.deviceId,
                "projectId" to log.projectId,
                "timestamp" to log.timestamp,
                "data" to mapOf(
                    "pm25" to mapOf(
                        "value" to log.pm25Value,
                        "level" to log.pm25Level
                    ),
                    "pm10" to mapOf(
                        "value" to log.pm10Value,
                        "level" to log.pm10Level
                    ),
                    "temperature" to mapOf(
                        "value" to log.temperature,
                        "level" to log.temperatureLevel
                    ),
                    "humidity" to mapOf(
                        "value" to log.humidity,
                        "level" to log.humidityLevel
                    ),
                    "co2" to mapOf(
                        "value" to log.co2Value,
                        "level" to log.co2Level
                    ),
                    "voc" to mapOf(
                        "value" to log.vocValue,
                        "level" to log.vocLevel
                    )
                )
            )
        }

        // 디버깅을 위해 예쁘게 출력
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload)
    }

    suspend fun uploadPendingLogs() {
        println("Uploading pending logs...")
        logFolder.listFiles()?.let { files ->
            if (files.isEmpty()) {
                println("No log files found")
                return
            }

            files.forEach { file ->
                try {
                    println("Processing file: ${file.name}")
                    val logs = parseCsvFile(file)
                    if (logs.isNotEmpty()) {
                        val jsonString = convertToJsonFormat(logs)
                        println("Sending payload: $jsonString")

                        val requestBody = jsonString.toRequestBody("application/json".toMediaType())
                        val response = apiService.uploadSensorLogs(requestBody)

                        if (response.success) {
                            file.delete()
                            println("Successfully uploaded and deleted log file: ${file.name}, processed count: ${response.processedCount}")
                        } else {
                            println("Failed to upload log file: ${file.name}, server response: ${response.message}")
                        }
                    } else {
                        println("Skipping empty file: ${file.name}")
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
                        // timestamp는 이미 ISO 8601 형식이므로 직접 사용
                        SensorDataLogDto(
                            deviceId = values[1],
                            projectId = values[2].toLong(),
                            timestamp = values[0],  // ISO 8601 형식 문자열 직접 사용
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
                        println("Error parsing line: $line, Error: ${e.message}")
                        null
                    }
                }
        } catch (e: Exception) {
            println("Error reading file: ${file.name}, Error: ${e.message}")
            emptyList()
        }
    }
}
