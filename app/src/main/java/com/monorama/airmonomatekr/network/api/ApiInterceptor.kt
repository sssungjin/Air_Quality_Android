package com.monorama.airmonomatekr.network.api

import okhttp3.Interceptor
import okhttp3.Response
import com.monorama.airmonomatekr.util.TokenManager

class ApiInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()

        val requestBuilder = originalRequest.newBuilder().apply {
            if (token != null) {
                header("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
} 