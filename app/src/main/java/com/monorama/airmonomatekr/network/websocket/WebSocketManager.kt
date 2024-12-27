package com.monorama.airmonomatekr.network.websocket

import android.content.Context
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.util.Constants
import com.monorama.airmonomatekr.util.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val locationManager: LocationManager
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> get() = _isConnected

    private var lastSendTime: Long = 0
    private val SEND_INTERVAL = Constants.UploadInterval.SECOND // 10초

    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    fun connect(url: String, messageHandler: (String) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isConnected.value = true
                val deviceId = getDeviceId()
                val subscribeMessage = JsonObject().apply {
                    addProperty("type", "SUBSCRIBE")
                    add("payload", JsonObject().apply {
                        addProperty("topic", "device/$deviceId")
                    })
                }
                webSocket.send(subscribeMessage.toString())
                println("WebSocketManager: Sent subscribe message: $subscribeMessage")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                messageHandler(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("WebSocketManager: Connection failed: ${t.message}")
                println("WebSocketManager: Response: ${response?.message} - ${response?.body}")
                t.printStackTrace()
                _isConnected.value = false
            }
        })
    }

    fun sendSensorData(data: SensorLogData) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSendTime < SEND_INTERVAL) {
            println("WebSocketManager: Cannot send data - interval not reached")
            return // 10초가 지나지 않았으면 전송하지 않음
        }

        if (_isConnected.value) {
            coroutineScope.launch {
                try {
                    val deviceId = getDeviceId()
                    val location = locationManager.getCurrentLocation()
                    val settings = settingsDataStore.userSettings.first()

                    if (settings.projectId.isEmpty()) {
                        println("WebSocketManager: ProjectId is empty, cannot send data")
                        return@launch
                    }

                    // 한국 시간으로 현재 시간 생성
                    val timestamp = ZonedDateTime
                        .now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

                    // 한국 시간대 설정 주석 처리
                    // val koreaTimestamp = ZonedDateTime
                    //     .now(ZoneId.of("Asia/Seoul"))
                    //     .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

                    val sensorDataJson = JsonObject().apply {
                        add("co2", createSensorValueJson(data.co2))
                        add("humidity", createSensorValueJson(data.humidity))
                        add("pm10", createSensorValueJson(data.pm10))
                        add("pm25", createSensorValueJson(data.pm25))
                        add("temperature", createSensorValueJson(data.temperature))
                        add("voc", createSensorValueJson(data.voc))
                        // timestamp를 제거하거나 동일한 형식으로 변경
                    }

                    val message = JsonObject().apply {
                        addProperty("type", "SENSOR_DATA")
                        add("payload", JsonObject().apply {
                            addProperty("deviceId", deviceId)
                            addProperty("projectId", settings.projectId.toLong())
                            addProperty("timestamp", timestamp)
                            add("data", sensorDataJson)
                            location?.let { (lat, lng) ->
                                addProperty("latitude", lat)
                                addProperty("longitude", lng)
                            }
                        })
                    }
                    webSocket?.send(message.toString())
                    println("WebSocketManager: Sent sensor data: $message")
                    lastSendTime = currentTime // 전송 시간 업데이트
                } catch (e: Exception) {
                    println("WebSocketManager: Error sending sensor data: ${e.message}")
                }
            }
        } else {
            println("WebSocketManager: Cannot send data - not connected")
        }
    }

    private fun createSensorValueJson(sensorValue: SensorLogData.SensorValue): JsonObject {
        return JsonObject().apply {
            addProperty("value", sensorValue.value)
            addProperty("level", sensorValue.level)
        }
    }

    fun disconnect() {
        if (_isConnected.value) {
            val deviceId = getDeviceId()
            val unsubscribeMessage = JsonObject().apply {
                addProperty("type", "UNSUBSCRIBE")
                add("payload", JsonObject().apply {
                    addProperty("topic", "device/$deviceId")
                })
            }
            webSocket?.send(unsubscribeMessage.toString())
            println("WebSocketManager: Sent unsubscribe message: $unsubscribeMessage")
        }

        // 웹소켓 연결은 유지
        // _isConnected.value = false
        println("WebSocketManager: Unsubscribed successfully, connection remains open")
    }

    fun unsubscribe(deviceId: String) {
        val unsubscribeMessage = JsonObject().apply {
            addProperty("type", "UNSUBSCRIBE")
            add("payload", JsonObject().apply {
                addProperty("topic", "device/$deviceId")
            })
        }
        webSocket?.send(unsubscribeMessage.toString())
        println("WebSocketManager: Sent unsubscribe message: $unsubscribeMessage")
    }

}