package com.monorama.airmonomatekr.network.api.dto

data class DeviceResponseDto(
    val deviceId: String,
    val userName: String,
    val userEmail: String,
    val projectId: Long?
)