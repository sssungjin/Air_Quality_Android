package com.monorama.airmonomatekr.network.api.dto

data class SensorDataLogDto(
    val deviceId: String,
    val projectId: Long,
    val timestamp: String,
    val pm25Value: Double,
    val pm25Level: Int,
    val pm10Value: Double,
    val pm10Level: Int,
    val temperature: Double,
    val temperatureLevel: Int,
    val humidity: Double,
    val humidityLevel: Int,
    val co2Value: Double,
    val co2Level: Int,
    val vocValue: Double,
    val vocLevel: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rawData: ByteArray? = null
)