package com.monorama.airmonomatekr.data.model

data class UserSettings(
    val projectId: String = "",
    val userName: String = "",
    val email: String = "",
    val transmissionMode: TransmissionMode = TransmissionMode.REALTIME,
    val uploadInterval: Int = 5
)