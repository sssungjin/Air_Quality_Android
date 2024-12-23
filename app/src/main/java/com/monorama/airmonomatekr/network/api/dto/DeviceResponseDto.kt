package com.monorama.airmonomatekr.network.api.dto

import com.monorama.airmonomatekr.data.model.DeviceLocation
import com.monorama.airmonomatekr.data.model.TransmissionMode

data class DeviceResponseDto(
    val deviceId: String,
    val userName: String,
    val userEmail: String,
    val projectId: Long?,
    val location: DeviceLocation? = null,
    val transmissionMode: TransmissionMode,
    val uploadInterval: Int
)