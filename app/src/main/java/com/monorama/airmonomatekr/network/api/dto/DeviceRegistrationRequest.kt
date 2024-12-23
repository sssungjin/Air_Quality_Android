package com.monorama.airmonomatekr.network.api.dto

import com.monorama.airmonomatekr.data.model.TransmissionMode

data class DeviceRegistrationRequest(
    val deviceId: String,
    val projectId: Long,
    val userName: String,
    val userEmail: String,
    val transmissionMode: TransmissionMode,
    val uploadInterval: Int
)