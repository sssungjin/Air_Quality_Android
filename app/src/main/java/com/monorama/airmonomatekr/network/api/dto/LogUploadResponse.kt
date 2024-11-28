package com.monorama.airmonomatekr.network.api.dto

data class LogUploadResponse(
    val success: Boolean,
    val message: String,
    val processedCount: Int
)