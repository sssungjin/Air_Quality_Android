package com.monorama.airmonomatekr.network.api

import com.monorama.airmonomatekr.network.api.dto.ApiResponse
import com.monorama.airmonomatekr.data.model.SearchRequest
import com.monorama.airmonomatekr.network.api.dto.DeviceLocationRequest
import com.monorama.airmonomatekr.network.api.dto.DeviceLocationResponse
import com.monorama.airmonomatekr.network.api.dto.DeviceRegistrationRequest
import com.monorama.airmonomatekr.network.api.dto.DeviceRegistrationResponse
import com.monorama.airmonomatekr.network.api.dto.DeviceResponseDto
import com.monorama.airmonomatekr.network.api.dto.LogUploadResponse
import com.monorama.airmonomatekr.network.api.dto.ProjectResponse
import com.monorama.airmonomatekr.network.api.dto.SensorDataPayload
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("devices/search")
    suspend fun searchDevices(
        @Body request: SearchRequest
    ): ApiResponse

    @GET("devices/{deviceId}")
    suspend fun getDevice(
        @Path("deviceId") deviceId: String
    ): DeviceResponseDto

    @POST("devices/{deviceId}/register")
    suspend fun registerDevice(
        @Path("deviceId") deviceId: String,
        @Body request: DeviceRegistrationRequest
    ): DeviceRegistrationResponse

    @GET("projects")
    suspend fun getProjects(): ProjectResponse

    @PUT("devices/{deviceId}/location")
    suspend fun updateDeviceLocation(
        @Path("deviceId") deviceId: String,
        @Body request: DeviceLocationRequest
    ): DeviceLocationResponse

//     @POST("sensor-data/logs")
//     suspend fun uploadSensorLogs(
//         @Body logs: List<SensorDataLogDto>
//     ): LogUploadResponse

    @POST("sensor-data/batch")
    @Headers("Content-Type: application/json")
    suspend fun uploadSensorLogs(@Body payload: RequestBody): LogUploadResponse

    @GET("sensor-data/history/{deviceId}")
    suspend fun getDeviceSensorHistory(
        @Path("deviceId") deviceId: String,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): ApiResponse
}