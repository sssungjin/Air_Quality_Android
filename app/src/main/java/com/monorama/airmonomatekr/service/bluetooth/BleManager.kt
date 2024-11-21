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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.monorama.airmonomatekr.data.repository.SensorData
import com.monorama.airmonomatekr.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class BleManager(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

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

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            println("BleManager: Connection state changed - status: $status, newState: $newState")

            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            println("BleManager: Successfully connected to GATT server")
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                println("BleManager: Discovering services...")
                                // 서비스 발견 지연 추가
                                Thread.sleep(600)
                                gatt.discoverServices()
                            }
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            println("BleManager: Disconnected from GATT server")
                            bluetoothGatt = null
                            _sensorData.value = null
                        }
                    }
                }
                else -> {
                    println("BleManager: Connection state change failed with status: $status")
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    _sensorData.value = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("BleManager: Services discovered successfully")
                println("BleManager: Found ${gatt.services.size} services")
                gatt.services.forEach { service ->
                    println("BleManager: Service: ${service.uuid}")
                }
                enableSensors(gatt)
                setupNotifications(gatt)
            } else {
                println("BleManager: Service discovery failed with status: $status")
            }
        }
    }

    private fun parseSensorData(value: ByteArray) {
        if (value.size < 18) return

        try {
            val data = SensorData(
                pm25 = SensorData.SensorValue(
                    value = ((value[0].toInt() and 0xFF) shl 8 or (value[1].toInt() and 0xFF)).toFloat(),
                    level = value[2].toInt() and 0xFF
                ),
                pm10 = SensorData.SensorValue(
                    value = ((value[3].toInt() and 0xFF) shl 8 or (value[4].toInt() and 0xFF)).toFloat(),
                    level = value[5].toInt() and 0xFF
                ),
                temperature = SensorData.SensorValue(
                    value = ((value[6].toInt() and 0xFF) shl 8 or (value[7].toInt() and 0xFF)) / 10.0f,
                    level = value[8].toInt() and 0xFF
                ),
                humidity = SensorData.SensorValue(
                    value = ((value[9].toInt() and 0xFF) shl 8 or (value[10].toInt() and 0xFF)) / 10.0f,
                    level = value[11].toInt() and 0xFF
                ),
                co2 = SensorData.SensorValue(
                    value = ((value[12].toInt() and 0xFF) shl 8 or (value[13].toInt() and 0xFF)).toFloat(),
                    level = value[14].toInt() and 0xFF
                ),
                voc = SensorData.SensorValue(
                    value = ((value[15].toInt() and 0xFF) shl 8 or (value[16].toInt() and 0xFF)).toFloat(),
                    level = value[17].toInt() and 0xFF
                )
            )
            _sensorData.value = data
            println("BleManager: Parsed sensor data: $data")
        } catch (e: Exception) {
            println("Error parsing sensor data: ${e.message}")
        }
    }

    fun startScan(onDevicesFound: (List<BluetoothDevice>) -> Unit) {
        println("BleManager: Starting scan...")

        // Android 버전에 따른 권한 체크
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
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

        println("BleManager: Starting BLE scan with settings: ${scanSettings.scanMode}")

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

        if (!PermissionHelper.hasRequiredPermissions(context)) {
            println("BleManager: Required permissions not granted")
            throw SecurityException("Required permissions are not granted")
        }

        // 기존 연결이 있다면 해제
        bluetoothGatt?.let {
            println("BleManager: Disconnecting from previous connection")
            disconnect()
        }

        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(deviceAddress)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    println("BleManager: Connecting to device: ${device.address}")
                    // autoConnect를 true로 설정하여 연결 안정성 향상
                    bluetoothGatt = device.connectGatt(context, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
                    return true
                } else {
                    println("BleManager: BLUETOOTH_CONNECT permission not granted")
                }
            } catch (e: IllegalArgumentException) {
                println("BleManager: Failed to connect: ${e.message}")
                return false
            }
        }
        println("BleManager: BluetoothAdapter not available")
        return false
    }


    private fun setupNotifications(gatt: BluetoothGatt) {
        if (!PermissionHelper.hasBluetoothPermissions(context)) {
            return
        }

        try {
            gatt.getService(UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb"))?.let { service ->
                service.getCharacteristic(UUID.fromString("0000ffb3-0000-1000-8000-00805f9b34fb"))?.let { characteristic ->
                    println("BleManager: Setting up notifications for sensor data")
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
                    gatt.setCharacteristicNotification(characteristic, true)
                }
            }
        } catch (e: Exception) {
            println("BleManager: Error setting up notifications: ${e.message}")
        }
    }

    private fun enableSensors(gatt: BluetoothGatt) {
        if (!PermissionHelper.hasBluetoothPermissions(context)) {
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                // Enable dust sensor
                gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))?.let { service ->
                    service.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))?.let { characteristic ->
                        println("BleManager: Enabling dust sensor")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(
                                characteristic,
                                byteArrayOf(0x01),
                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            )
                        } else {
                            // API 레벨 28용 레거시 방식
                            characteristic.value = byteArrayOf(0x01)
                            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            gatt.writeCharacteristic(characteristic)
                        }
                    }
                }

                // Enable gas sensor
                gatt.getService(UUID.fromString("0000ffd0-0000-1000-8000-00805f9b34fb"))?.let { service ->
                    service.getCharacteristic(UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb"))?.let { characteristic ->
                        println("BleManager: Enabling gas sensor")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(
                                characteristic,
                                byteArrayOf(0x01),
                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            )
                        } else {
                            characteristic.value = byteArrayOf(0x01)
                            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            gatt.writeCharacteristic(characteristic)
                        }
                    }
                }

                // Enable temperature/humidity sensor
                gatt.getService(UUID.fromString("0000ffc0-0000-1000-8000-00805f9b34fb"))?.let { service ->
                    service.getCharacteristic(UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb"))?.let { characteristic ->
                        println("BleManager: Enabling temperature/humidity sensor")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(
                                characteristic,
                                byteArrayOf(0x02),
                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            )
                        } else {
                            characteristic.value = byteArrayOf(0x02)
                            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            gatt.writeCharacteristic(characteristic)
                        }
                    }
                }
            } catch (e: Exception) {
                println("BleManager: Error enabling sensors: ${e.message}")
            }
        }
    }

    fun disconnect() {
        if (!PermissionHelper.hasBluetoothPermissions(context)) {
            return
        }

        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                println("BleManager: Disconnecting from device")
                gatt.disconnect()
                gatt.close()
                bluetoothGatt = null
            }
        }
    }
}