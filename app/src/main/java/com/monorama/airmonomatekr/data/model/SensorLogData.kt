package com.monorama.airmonomatekr.data.model

data class SensorLogData(
    val pm25: SensorValue,
    val pm10: SensorValue,
    val temperature: SensorValue,
    val humidity: SensorValue,
    val co2: SensorValue,
    val voc: SensorValue,
    val timestamp: Long = System.currentTimeMillis()
) {
    data class SensorValue(
        val value: Float,
        val level: Int
    )
}