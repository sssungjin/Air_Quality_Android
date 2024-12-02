package com.monorama.airmonomatekr.network.api.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
data class SensorDataPayload(
    val deviceId: String,
    val projectId: Long,
    val timestamp: String,
    val data: SensorData,
    val latitude: Double?,
    val longitude: Double?
) {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    data class SensorData(
        val pm25: SensorValue,
        val pm10: SensorValue,
        val temperature: SensorValue,
        val humidity: SensorValue,
        val co2: SensorValue,
        val voc: SensorValue
    )

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    data class SensorValue(
        val value: Double,
        val level: Int
    )
}