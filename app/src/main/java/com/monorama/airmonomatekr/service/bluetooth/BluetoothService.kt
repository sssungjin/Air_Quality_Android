package com.monorama.airmonomatekr.service.bluetooth

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.monorama.airmonomatekr.MainActivity
import com.monorama.airmonomatekr.R
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import com.monorama.airmonomatekr.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class BluetoothService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var bleManager: BleManager

    @Inject
    lateinit  var webSocketManager: WebSocketManager
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        bleManager = BleManager(this, webSocketManager)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> startForegroundService()
            ACTION_STOP_FOREGROUND -> stopForegroundService()
            ACTION_CONNECT -> {
                intent.getStringExtra(EXTRA_DEVICE_ADDRESS)?.let { address ->
                    connectDevice(address)
                }
            }
            ACTION_DISCONNECT -> disconnectDevice()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun connectDevice(address: String) {
        if (!PermissionHelper.hasRequiredPermissions(this)) {
            _isConnected.value = false
            return
        }

        serviceScope.launch {
            try {
                val connected = bleManager.connect(address)
                _isConnected.value = connected
                updateNotification()
            } catch (e: Exception) {
                _isConnected.value = false
            }
        }
    }

    private fun disconnectDevice() {
        serviceScope.launch {
            bleManager.disconnect()
            _isConnected.value = false
            updateNotification()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bluetooth Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Quality Monitor")
            .setContentText(if (_isConnected.value) "Connected" else "Disconnected")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "BluetoothServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_FOREGROUND = "START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "STOP_FOREGROUND"
        const val ACTION_CONNECT = "CONNECT"
        const val ACTION_DISCONNECT = "DISCONNECT"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }
} 