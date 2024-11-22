package com.monorama.airmonomatekr.service.bluetooth

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import com.monorama.airmonomatekr.util.Constants
import com.monorama.airmonomatekr.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class BleManager @Inject constructor(
    private val context: Context,
    private val webSocketManager: WebSocketManager
) {
    init {
        // 웹소켓 연결 초기화
        webSocketManager.connect(Constants.Api.WS_URL) { message ->
            println("BleManager: Received WebSocket message: $message")
        }
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val _sensorLogData = MutableStateFlow<SensorLogData?>(null)
    val sensorLogData: StateFlow<SensorLogData?> = _sensorLogData.asStateFlow()

    private val bluetoothLeScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    companion object {
        private const val DEVICE_NAME = "Bandi-Pico"
        private val SERVICE_UUIDS = listOf(
            "0000180a-0000-1000-8000-00805f9b34fb", // Device Information Service
            "0000ffe0-0000-1000-8000-00805f9b34fb",
            "0000ffb0-0000-1000-8000-00805f9b34fb",
            "0000ffd0-0000-1000-8000-00805f9b34fb",
            "0000ffc0-0000-1000-8000-00805f9b34fb"
        )
    }

    private val scanFilter = ScanFilter.Builder()
        .setDeviceName(DEVICE_NAME)
        .build()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            println("BleManager: Scan result received")
            println("BleManager: Device name: ${result.device.name}")
            println("BleManager: Device address: ${result.device.address}")
            println("BleManager: Device RSSI: ${result.rssi}")

            val device = result.device
            // SDK 28에서는 BLUETOOTH_CONNECT 권한 체크를 완전히 건너뜀
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || 
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name?.let { name ->
                    if (name == DEVICE_NAME) {
                        println("BleManager: Found Bandi-Pico device!")
                        if (!discoveredDevices.any { it.address == device.address }) {
                            discoveredDevices.add(device)
                            println("BleManager: Added device to list. Current devices: ${discoveredDevices.map { it.name }}")
                            currentScanCallback?.invoke(discoveredDevices.toList())
                        }
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("BleManager: Scan failed with error code: $errorCode")
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> println("BleManager: Scan already started")
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> println("BleManager: Application registration failed")
                SCAN_FAILED_FEATURE_UNSUPPORTED -> println("BleManager: BLE scanning not supported")
                SCAN_FAILED_INTERNAL_ERROR -> println("BleManager: Internal error")
                else -> println("BleManager: Unknown error")
            }
        }
    }

    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private var currentScanCallback: ((List<BluetoothDevice>) -> Unit)? = null

    private var dataCollectionJob: Job? = null

    private fun startDataCollection(gatt: BluetoothGatt) {
        println("BleManager: Starting data collection")
        dataCollectionJob?.cancel()

        dataCollectionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // 센서 데이터 특성 읽기
                    val service = gatt.getService(UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb"))
                    val characteristic = service?.getCharacteristic(UUID.fromString("0000ffb3-0000-1000-8000-00805f9b34fb"))
                    
                    if (characteristic == null) {
                        println("BleManager: Characteristic not found")
                        break
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            println("BleManager: Reading characteristic...")
                            gatt.readCharacteristic(characteristic)
                        }
                    } else {
                        // SDK 28용 레거시 방식
                        println("BleManager: Reading characteristic (legacy)...")
                        gatt.readCharacteristic(characteristic)
                    }
                    
                    delay(1000) // 1초 대기
                } catch (e: Exception) {
                    println("BleManager: Error reading data: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            println("BleManager: Connection state changed - status: $status, newState: $newState")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    println("BleManager: Successfully connected to GATT server")

                    CoroutineScope(Dispatchers.IO).launch {
                        delay(1000) // 1초 대기

                        // Android 12 (API 31) 이상
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                println("BleManager: Starting service discovery")
                                if (!gatt.discoverServices()) {
                                    println("BleManager: Failed to start service discovery")
                                    disconnect()
                                }
                            } else {
                                println("BleManager: Missing BLUETOOTH_CONNECT permission")
                                disconnect()
                            }
                        }
                        // Android 11 이하 (API 30 이하)
                        else {
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                println("BleManager: Starting service discovery (legacy)")
                                if (!gatt.discoverServices()) {
                                    println("BleManager: Failed to start service discovery")
                                    disconnect()
                                }
                            } else {
                                println("BleManager: Missing location permission")
                                disconnect()
                            }
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    println("BleManager: Disconnected from GATT server")
                    dataCollectionJob?.cancel()
                    closeGatt()
                    _sensorLogData.value = null
                }
            }
        }

        private fun closeGatt() {
            // Android 12 (API 31) 이상
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }
            // Android 11 이하 (API 30 이하)
            else {
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            println("BleManager: onServicesDiscovered status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("BleManager: Services discovered successfully")
                
                // 서비스 목록 출력
                gatt.services.forEach { service ->
                    println("BleManager: Found service: ${service.uuid}")
                    service.characteristics.forEach { characteristic ->
                        println("BleManager: - Characteristic: ${characteristic.uuid}")
                    }
                }

                // 순차적으로 센서 설정
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        println("BleManager: Setting up device...")
                        enableSensors(gatt)
                        delay(1000)
                        setupNotifications(gatt)
                        delay(1000)
                        startDataCollection(gatt)
                        println("BleManager: Device setup complete")
                    } catch (e: Exception) {
                        println("BleManager: Error during device setup: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } else {
                println("BleManager: Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            println("BleManager: onCharacteristicRead status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("BleManager: [Legacy] Read characteristic success")
                characteristic.value?.let { value ->
                    println("BleManager: [Legacy] Received data length: ${value.size}")
                    parseSensorData(value)
                }
            } else {
                println("BleManager: [Legacy] Read characteristic failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            println("BleManager: Characteristic changed")
            characteristic.value?.let { value ->
                println("BleManager: Received data length: ${value.size}")
                parseSensorData(value)
            }
        }
    }

    private fun parseSensorData(value: ByteArray) {
        if (value.size < 18) return

        try {
            val data = SensorLogData(
                pm25 = SensorLogData.SensorValue(
                    value = ((value[0].toInt() and 0xFF) shl 8 or (value[1].toInt() and 0xFF)).toFloat(),
                    level = value[2].toInt() and 0xFF
                ),
                pm10 = SensorLogData.SensorValue(
                    value = ((value[3].toInt() and 0xFF) shl 8 or (value[4].toInt() and 0xFF)).toFloat(),
                    level = value[5].toInt() and 0xFF
                ),
                temperature = SensorLogData.SensorValue(
                    value = ((value[6].toInt() and 0xFF) shl 8 or (value[7].toInt() and 0xFF)) / 10.0f,
                    level = value[8].toInt() and 0xFF
                ),
                humidity = SensorLogData.SensorValue(
                    value = ((value[9].toInt() and 0xFF) shl 8 or (value[10].toInt() and 0xFF)) / 10.0f,
                    level = value[11].toInt() and 0xFF
                ),
                co2 = SensorLogData.SensorValue(
                    value = ((value[12].toInt() and 0xFF) shl 8 or (value[13].toInt() and 0xFF)).toFloat(),
                    level = value[14].toInt() and 0xFF
                ),
                voc = SensorLogData.SensorValue(
                    value = ((value[15].toInt() and 0xFF) shl 8 or (value[16].toInt() and 0xFF)).toFloat(),
                    level = value[17].toInt() and 0xFF
                )
            )
            _sensorLogData.value = data

            // 웹소켓으로 데이터 전송
            webSocketManager.sendSensorData(data)

            println("BleManager: Parsed and sent sensor data: $data")
        } catch (e: Exception) {
            println("Error parsing sensor data: ${e.message}")
        }
    }

    fun startScan(onDevicesFound: (List<BluetoothDevice>) -> Unit) {
        println("BleManager: Starting scan...")

        // Android 12 (API 31) 이상
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        }
        // Android 11 이하 (API 30 이하)
        else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            println("BleManager: Required permission not granted")
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!bluetoothManager.adapter?.isEnabled!!) {
            println("BleManager: Bluetooth is not enabled")
            return
        }

        discoveredDevices.clear()
        currentScanCallback = onDevicesFound

        try {
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            println("BleManager: Scan started successfully")
        } catch (e: Exception) {
            println("BleManager: Failed to start scan: ${e.message}")
            e.printStackTrace()
        }
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeScanner?.stopScan(scanCallback)
            println("BleManager: Scan stopped")
        }
        currentScanCallback = null
    }

    fun connect(deviceAddress: String): Boolean {
        println("BleManager: Attempting to connect to device: $deviceAddress")

        // 이전 연결 해제
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        bluetoothGatt?.close()
        bluetoothGatt = null

        // 새로운 연결 시도
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(deviceAddress)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    println("BleManager: Creating GATT connection")
                    bluetoothGatt = device.connectGatt(
                        context,
                        false,  // autoConnect false for immediate connection attempt
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                    return true
                } else {
                    println("BleManager: Missing BLUETOOTH_CONNECT permission")
                }
            } catch (e: Exception) {
                println("BleManager: Connection error: ${e.message}")
            }
        }

        return false
    }


    private fun setupNotifications(gatt: BluetoothGatt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !PermissionHelper.hasBluetoothPermissions(context)
        ) {
            println("BleManager: Cannot setup notifications - missing permissions")
            return
        }

        try {
            val sensorService = gatt.getService(UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb"))
            val sensorCharacteristic = sensorService?.getCharacteristic(
                UUID.fromString("0000ffb3-0000-1000-8000-00805f9b34fb")
            )

            if (sensorCharacteristic != null) {
                println("BleManager: Found sensor characteristic")

                // 노티피케이션 활성화
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    gatt.setCharacteristicNotification(sensorCharacteristic, true)
                } else {
                    // Legacy 방식
                    sensorCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    gatt.setCharacteristicNotification(sensorCharacteristic, true)
                }

                // 디스크립터 설정
                val descriptor = sensorCharacteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    println("BleManager: Enabled notifications for sensor characteristic")
                }
            } else {
                println("BleManager: Could not find sensor characteristic")
            }
        } catch (e: Exception) {
            println("BleManager: Error setting up notifications: ${e.message}")
        }
    }

    private suspend fun enableSensors(gatt: BluetoothGatt) {
        println("BleManager: Starting to enable sensors")

        if (!PermissionHelper.hasBluetoothPermissions(context)) {
            println("BleManager: Missing permissions for enabling sensors")
            return
        }

        try {
            // Dust sensor (PM2.5)
            println("BleManager: Attempting to enable dust sensor")
            val dustService = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
            val dustChar = dustService?.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))

            if (dustChar != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val success = if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    } else {

                    }
                    gatt.writeCharacteristic(
                        dustChar,
                        byteArrayOf(0x01),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                    println("BleManager: Dust sensor enable request sent: $success")
                } else {
                    dustChar.value = byteArrayOf(0x01)
                    dustChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val success = gatt.writeCharacteristic(dustChar)
                    println("BleManager: Dust sensor enable request sent (legacy): $success")
                }
                delay(500)  // 각 작업 사이의 지연 시간
            } else {
                println("BleManager: Dust sensor characteristic not found")
            }

            // Gas sensor (CO2)
            println("BleManager: Attempting to enable gas sensor")
            val gasService = gatt.getService(UUID.fromString("0000ffd0-0000-1000-8000-00805f9b34fb"))
            val gasChar = gasService?.getCharacteristic(UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb"))

            if (gasChar != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val success = gatt.writeCharacteristic(
                        gasChar,
                        byteArrayOf(0x01),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                    println("BleManager: Gas sensor enable request sent: $success")
                } else {
                    gasChar.value = byteArrayOf(0x01)
                    gasChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val success = gatt.writeCharacteristic(gasChar)
                    println("BleManager: Gas sensor enable request sent (legacy): $success")
                }
                delay(500)
            } else {
                println("BleManager: Gas sensor characteristic not found")
            }

            // Temperature/Humidity sensor
            println("BleManager: Attempting to enable temperature/humidity sensor")
            val thService = gatt.getService(UUID.fromString("0000ffc0-0000-1000-8000-00805f9b34fb"))
            val thChar = thService?.getCharacteristic(UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb"))

            if (thChar != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val success = gatt.writeCharacteristic(
                        thChar,
                        byteArrayOf(0x02),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                    println("BleManager: Temperature sensor enable request sent: $success")
                } else {
                    thChar.value = byteArrayOf(0x02)
                    thChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val success = gatt.writeCharacteristic(thChar)
                    println("BleManager: Temperature sensor enable request sent (legacy): $success")
                }
            } else {
                println("BleManager: Temperature sensor characteristic not found")
            }

        } catch (e: Exception) {
            println("BleManager: Error enabling sensors: ${e.message}")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        println("BleManager: Disconnecting...")
        dataCollectionJob?.cancel()

        if (!PermissionHelper.hasBluetoothPermissions(context)) {
            return
        }

        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt.disconnect()
                gatt.close()
                bluetoothGatt = null
                _sensorLogData.value = null

                // 웹소켓 연결 해제
                webSocketManager.disconnect()
            }
        }
    }
}