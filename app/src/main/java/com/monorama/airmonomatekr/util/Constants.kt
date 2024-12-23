package com.monorama.airmonomatekr.util

import com.monorama.airmonomatekr.BuildConfig

object Constants {
    const val API_KEY = BuildConfig.API_KEY

//    object Api {
//        const val BASE_URL = "https://air.monomate.kr/api/v1/"
//        const val WS_URL = "https://air.monomate.kr/ws"
//
//        //const val BASE_URL = "http://192.168.0.20:8080/api/v1/"
//    }
    object Api {
    // 로컬 서버의 API URL
        const val BASE_URL = "http://10.0.2.2:8080/api/v1/" // Android Emulator에서 localhost 접근
        // const val BASE_URL = "http://192.168.0.20:8080/api/v1/" // 실제 기기에서 접근할 경우

        const val WS_URL = "http://10.0.2.2:8080/ws" // WebSocket URL
    }

    object Alarm {
        const val MINUTE_INTERVAL = 5L  // 5분 간격
        const val DAILY_INTERVAL = 24L  // 24시간 간격
    }

    object WebUrl {
        const val WEB_URL = "https://air.monomate.kr"
    }

    object UploadInterval {
        const val SECOND = 10000L
        const val MINUTE = 1
        const val HOUR = 2
        const val DAY = 3
    }
}