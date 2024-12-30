package com.monorama.airmonomatekr.service.bluetooth

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.monorama.airmonomatekr.MainActivity
import com.monorama.airmonomatekr.R
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import com.monorama.airmonomatekr.util.PermissionHelper
import com.monorama.airmonomatekr.util.SensorLogManager
import com.monorama.airmonomatekr.util.WorkerScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var bleManager: BleManager

    @Inject
    lateinit var webSocketManager: WebSocketManager

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var sensorLogManager: SensorLogManager

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleManager = BleManager(this, webSocketManager, settingsDataStore, sensorLogManager, workerScheduler)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> startForegroundService()
            ACTION_STOP_FOREGROUND -> stopForegroundService()
            ACTION_CONNECT -> {
                intent.getStringExtra(EXTRA_DEVICE_ADDRESS)?.let { address ->
                    val device = bluetoothAdapter.getRemoteDevice(address)
                    connectDevice(device)
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
        try {
            // 1. BLE 연결 해제 재확인
            bleManager.disconnect()

            // 2. 웹소켓 연결 해제
            webSocketManager.disconnect()

            // 3. 서비스 상태 초기화
            _isConnected.value = false

            // 4. 블루투스 어댑터 재설정 시도
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothManager.adapter?.cancelDiscovery()
                }
            } else {
                @Suppress("DEPRECATION")
                bluetoothManager.adapter?.cancelDiscovery()
            }

            // 5. 포그라운드 서비스 중지
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }

            // 6. 서비스 자체를 중지
            stopSelf()
        } catch (e: Exception) {
            println("BluetoothService: Error stopping service: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun connectDevice(device: BluetoothDevice) {
        if (!PermissionHelper.hasRequiredPermissions(this)) {
            _isConnected.value = false
            return
        }

        serviceScope.launch {
            try {
                val connected = bleManager.connect(device)
                _isConnected.value = connected
                updateNotification()
            } catch (e: Exception) {
                _isConnected.value = false
            }
        }
    }

    private fun disconnectDevice() {
        serviceScope.launch {
            try {
                println("BluetoothService: Starting device disconnection...")

                // BLE 연결 해제
                bleManager.disconnect()

                // 웹소켓에서 구독 해제
                val deviceId = bleManager.getDeviceId() // 현재 연결된 디바이스 ID 가져오기
                webSocketManager.unsubscribe(deviceId)

                // 서비스 상태 초기화
                _isConnected.value = false

                // 포그라운드 서비스도 중지
                stopForegroundService()

                println("BluetoothService: Device disconnection completed")
            } catch (e: Exception) {
                println("BluetoothService: Error during device disconnection: ${e.message}")
                _isConnected.value = false
                e.printStackTrace()
            }
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