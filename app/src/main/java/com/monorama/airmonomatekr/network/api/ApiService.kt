 package com.monorama.airmonomatekr.network.api

 import com.monorama.airmonomatekr.data.model.ApiResponse
 import com.monorama.airmonomatekr.data.model.SearchRequest
 import com.monorama.airmonomatekr.network.api.dto.DeviceRegistrationRequest
 import com.monorama.airmonomatekr.network.api.dto.DeviceRegistrationResponse
 import com.monorama.airmonomatekr.network.api.dto.DeviceResponseDto
 import com.monorama.airmonomatekr.network.api.dto.ProjectResponse
 import retrofit2.http.Body
 import retrofit2.http.GET
 import retrofit2.http.POST
 import retrofit2.http.Path

 interface ApiService {
     @POST("/devices/search")
     suspend fun searchDevices(
         @Body request: SearchRequest
     ): ApiResponse

     @GET("/devices/{deviceId}")
     suspend fun getDevice(
         @Path("deviceId") deviceId: String
     ): DeviceResponseDto

     @POST("/devices/{deviceId}/register")
     suspend fun registerDevice(
         @Path("deviceId") deviceId: String,
         @Body request: DeviceRegistrationRequest
     ): DeviceRegistrationResponse

     @GET("/projects")
     suspend fun getProjects(): ProjectResponse
 }