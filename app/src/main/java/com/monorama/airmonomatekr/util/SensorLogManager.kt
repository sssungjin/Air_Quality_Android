package com.monorama.airmonomatekr.util

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.SensorDataLogDto
import com.monorama.airmonomatekr.util.LocationManager
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
    private val objectMapper: ObjectMapper,
    private val locationManager: LocationManager
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // ISO 8601 형식 포매터 설정
    //private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    // 시간대 설정 주석 처리
    // .apply {
    //     timeZone = TimeZone.getTimeZone("Asia/Seoul")
    // }
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    private val logFolder = File(context.filesDir, "sensor_logs")

    private var lastWriteTime: Long = 0
    private val WRITE_INTERVAL = Constants.UploadInterval.SECOND // 10초

    init {
        logFolder.mkdirs()
    }

    suspend fun writeLog(sensorData: SensorLogData, deviceId: String, projectId: Long) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastWriteTime < WRITE_INTERVAL) {
            return // 10초가 지나지 않았으면 저장하지 않음
        }

        val today = dateFormat.format(Date())
        val logFile = File(logFolder, "${today}_${deviceId}.csv")

        if (!logFile.exists()) {
            // 헤더 작성 (위도, 경도 추가)
            logFile.writeText("timestamp,deviceId,projectId,pm25Value,pm25Level,pm10Value,pm10Level," +
                    "temperature,temperatureLevel,humidity,humidityLevel,co2Value,co2Level,vocValue,vocLevel,latitude,longitude\n")
        }

        //val formattedTimestamp = timestampFormat.format(Date(sensorData.timestamp))

        // 위도, 경도 정보 가져오기
        val location = locationManager.getCurrentLocation()
        val latitude = location?.first ?: 0.0
        val longitude = location?.second ?: 0.0

        // 데이터 작성 (위도, 경도 추가)
        logFile.appendText("${timestampFormat.format(Date(sensorData.timestamp))},${deviceId},${projectId}," +
                "${sensorData.pm25.value},${sensorData.pm25.level}," +
                "${sensorData.pm10.value},${sensorData.pm10.level}," +
                "${sensorData.temperature.value},${sensorData.temperature.level}," +
                "${sensorData.humidity.value},${sensorData.humidity.level}," +
                "${sensorData.co2.value},${sensorData.co2.level}," +
                "${sensorData.voc.value},${sensorData.voc.level}," +
                "$latitude,$longitude\n")

        lastWriteTime = currentTime // 저장 시간 업데이트
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
                ),
                "location" to mapOf(  // 위치 정보 추가
                    "latitude" to log.latitude,
                    "longitude" to log.longitude
                )
            )
        }

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
                .drop(1)
                .mapNotNull { line ->
                    try {
                        val values = line.split(",")
                        // 위도, 경도 파싱 추가
                        SensorDataLogDto(
                            deviceId = values[1],
                            projectId = values[2].toLong(),
                            timestamp = values[0],
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
                            latitude = values[15].toDouble(),
                            longitude = values[16].toDouble()
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
