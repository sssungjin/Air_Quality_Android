package com.monorama.airmonomatekr.util

import com.monorama.airmonomatekr.BuildConfig

object Constants {
    const val API_KEY = BuildConfig.API_KEY

    object Api {
        const val BASE_URL = "https://air.monomate.kr/"
        const val WS_URL = "ws://air.monomate.kr/ws"
    }
}