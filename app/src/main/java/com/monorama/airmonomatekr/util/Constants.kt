package com.monorama.airmonomatekr.util

import com.monorama.airmonomatekr.BuildConfig

object Constants {
    const val API_KEY = BuildConfig.API_KEY

    object Api {
        const val BASE_URL = "https://air.monomate.kr/api/v1/"
        const val WS_URL = "https://air.monomate.kr/ws"

        //const val BASE_URL = "http://192.168.0.20:8080/api/v1/"
    }

    object Alarm {
        const val MINUTE_INTERVAL = 5L  // 5분 간격
        const val DAILY_INTERVAL = 24L  // 24시간 간격
    }
}