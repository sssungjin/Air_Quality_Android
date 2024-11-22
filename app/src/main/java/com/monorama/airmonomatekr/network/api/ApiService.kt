 package com.monorama.airmonomatekr.network.api

 import com.monorama.airmonomatekr.data.model.ApiResponse
 import com.monorama.airmonomatekr.data.model.SearchRequest
 import retrofit2.http.Body
 import retrofit2.http.POST

 import com.monorama.airmonomatekr.data.model.SensorLogData

 interface ApiService {
     @POST("api/v1/devices/search")
     suspend fun searchDevices(
         @Body request: SearchRequest
     ): ApiResponse
 }