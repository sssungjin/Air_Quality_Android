package com.monorama.airmonomatekr.network.api.dto

import com.monorama.airmonomatekr.data.model.DeviceLocation

data class DeviceLocationResponse(
    val success: Boolean,
    val message: String,
    val data: DeviceLocation?
)