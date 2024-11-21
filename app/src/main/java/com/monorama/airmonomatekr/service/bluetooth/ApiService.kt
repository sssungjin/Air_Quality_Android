package com.monorama.airmonomatekr.service.bluetooth

import com.monorama.airmonomatekr.data.repository.SensorData
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("sensor-data")
    suspend fun saveSensorData(@Body data: SensorData): SensorData
}