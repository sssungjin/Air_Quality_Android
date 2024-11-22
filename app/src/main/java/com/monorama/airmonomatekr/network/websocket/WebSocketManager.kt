package com.monorama.airmonomatekr.network.websocket

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.monorama.airmonomatekr.data.model.SensorLogData
import javax.inject.Singleton
import java.time.Instant

@Singleton
class WebSocketManager @Inject constructor(
    private val context: Context
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect(url: String, messageHandler: (String) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isConnected.value = true
                val deviceId = "L76w6X2s7zr7K2ClPvTNXg=="  // 디바이스 ID
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
        if (_isConnected.value) {
            val deviceId = "L76w6X2s7zr7K2ClPvTNXg=="  // 디바이스 ID
            val message = JsonObject().apply {
                addProperty("type", "SENSOR_DATA")
                add("payload", JsonObject().apply {
                    addProperty("deviceId", deviceId)
                    addProperty("timestamp", Instant.now().toString())
                    add("data", Gson().toJsonTree(data))
                    addProperty("latitude", 37.4992)
                    addProperty("longitude", 127.0619)
                })
            }
            webSocket?.send(message.toString())
            println("WebSocketManager: Sent sensor data: $message")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _isConnected.value = false
    }
}