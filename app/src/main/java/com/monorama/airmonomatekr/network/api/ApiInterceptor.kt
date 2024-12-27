package com.monorama.airmonomatekr.network.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import com.monorama.airmonomatekr.util.TokenManager
import kotlinx.coroutines.runBlocking

class ApiInterceptor @Inject constructor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // runBlocking을 사용하여 suspend 함수 호출
        val token = runBlocking { tokenManager.getToken() }

        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
} 