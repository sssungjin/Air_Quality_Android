package com.monorama.airmonomatekr.network.api.dto

data class DeviceLocationRequest(
    val floorLevel: Int,
    val placeType: String,
    val description: String
)