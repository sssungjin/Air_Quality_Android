package com.monorama.airmonomatekr.network.api

import com.monorama.airmonomatekr.data.model.ApiResponse
import com.monorama.airmonomatekr.data.model.SearchRequest
import com.monorama.airmonomatekr.data.model.SensorLogData
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/v1/devices/search")
    suspend fun searchDevices(
        @Body request: SearchRequest
    ): ApiResponse

    @POST("api/v1/sensor-data")
    suspend fun saveSensorData(
        @Body data: SensorLogData
    )
}