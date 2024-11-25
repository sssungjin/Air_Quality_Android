package com.monorama.airmonomatekr.data.model

data class SearchRequest(
    val location: Location? = null,
    val dateRange: DateRange,
    val apiKey: String
)