package com.monorama.airmonomatekr.network.api.dto

data class DeviceRegistrationRequest(
    val deviceId: String,
    val projectId: Long,
    val userName: String,
    val userEmail: String
)