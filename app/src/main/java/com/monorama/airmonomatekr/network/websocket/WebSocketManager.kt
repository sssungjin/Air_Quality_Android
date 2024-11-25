package com.monorama.airmonomatekr.network.websocket

import android.content.Context
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.SensorLogData
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
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

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

                    val message = JsonObject().apply {
                        addProperty("type", "SENSOR_DATA")
                        add("payload", JsonObject().apply {
                            addProperty("deviceId", deviceId)
                            addProperty("projectId", settings.projectId.toLong())  // projectId 추가
                            addProperty("timestamp", Instant.now().toString())
                            add("data", Gson().toJsonTree(data))
                            location?.let { (lat, lng) ->
                                addProperty("latitude", lat)
                                addProperty("longitude", lng)
                            }
                        })
                    }
                    webSocket?.send(message.toString())
                    println("WebSocketManager: Sent sensor data: $message")
                } catch (e: Exception) {
                    println("WebSocketManager: Error sending sensor data: ${e.message}")
                }
            }
        } else {
            println("WebSocketManager: Cannot send data - not connected")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _isConnected.value = false
    }
}