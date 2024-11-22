package com.monorama.airmonomatekr.data.model

data class ApiResponse(
    val content: List<SensorLogData>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
)