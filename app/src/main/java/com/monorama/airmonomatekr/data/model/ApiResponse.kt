package com.monorama.airmonomatekr.data.model

data class ApiResponse(
    val content: List<Any>,
    val pageable: PageableInfo,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
    val size: Int,
    val number: Int,
    val sort: SortInfo,
    val numberOfElements: Int,
    val first: Boolean,
    val empty: Boolean
)

data class PageableInfo(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: SortInfo,
    val offset: Long,
    val paged: Boolean,
    val unpaged: Boolean
)

data class SortInfo(
    val sorted: Boolean,
    val empty: Boolean,
    val unsorted: Boolean
)

data class SensorDataResponseDto(
    val deviceId: String,
    val timestamp: String,
    val pm25Value: Float,
    val pm25Level: Int,
    val pm10Value: Float,
    val pm10Level: Int,
    val temperature: Float,
    val temperatureLevel: Int,
    val humidity: Float,
    val humidityLevel: Int,
    val co2Value: Float,
    val co2Level: Int,
    val vocValue: Float,
    val vocLevel: Int,
    val latitude: Double?,
    val longitude: Double?
)