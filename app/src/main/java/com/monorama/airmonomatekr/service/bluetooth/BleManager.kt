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
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.monorama.airmonomatekr.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class BleManager(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val _sensorData = MutableStateFlow<ByteArray?>(null)
    val sensorData: StateFlow<ByteArray?> = _sensorData

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
            val device = result.device
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name?.let { name ->
                    if (name == DEVICE_NAME) {
                        println("Found device: $name (${device.address})")
                        discoveredDevices.add(device)
                        currentScanCallback?.invoke(discoveredDevices.toList())
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("BLE Scan Failed: Error Code $errorCode")
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> println("Scan already started")
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> println("Application registration failed")
                SCAN_FAILED_FEATURE_UNSUPPORTED -> println("BLE scanning not supported")
                SCAN_FAILED_INTERNAL_ERROR -> println("Internal error")
                else -> println("Unknown error")
            }
        }
    }

    fun startScan(onDevicesFound: (List<BluetoothDevice>) -> Unit) {
        if (!PermissionHelper.hasBluetoothPermissions(context)) {
            println("Bluetooth permissions not granted")
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager.adapter?.isEnabled != true) {
            println("Bluetooth is not enabled")
            return
        }

        discoveredDevices.clear()
        currentScanCallback = onDevicesFound

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            println("Starting BLE scan...")
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        }
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeScanner?.stopScan(scanCallback)
        }
        currentScanCallback = null
    }

    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private var currentScanCallback: ((List<BluetoothDevice>) -> Unit)? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (PermissionHelper.hasBluetoothPermissions(context)) {
                        if (ActivityCompat.checkSelfPermission(
                                context,  // this를 context로 변경
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    bluetoothGatt = null
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableSensors(gatt)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            _sensorData.value = value
        }
    }

    fun connect(deviceAddress: String): Boolean {
        // 필요한 모든 권한 체크
        if (!PermissionHelper.hasRequiredPermissions(context)) {
            throw SecurityException("Required permissions are not granted")
        }

        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(deviceAddress)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothGatt = device.connectGatt(context, false, gattCallback)
                    return true
                }
            } catch (e: IllegalArgumentException) {
                return false
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
                gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))?.let { service ->
                    service.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))?.let { characteristic ->
                        println("Enabling dust sensor...")
                        gatt.writeCharacteristic(characteristic, byteArrayOf(0x01),
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                gatt.getService(UUID.fromString("0000ffd0-0000-1000-8000-00805f9b34fb"))?.let { service ->
                    service.getCharacteristic(UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb"))?.let { characteristic ->
                        println("Enabling gas sensor...")
                        gatt.writeCharacteristic(characteristic, byteArrayOf(0x01),
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                gatt.getService(UUID.fromString("0000ffc0-0000-1000-8000-00805f9b34fb"))?.let { service ->
                    service.getCharacteristic(UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb"))?.let { characteristic ->
                        println("Enabling temperature/humidity sensor...")
                        gatt.writeCharacteristic(characteristic, byteArrayOf(0x02),
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }
            } catch (e: Exception) {
                println("Error enabling sensors: ${e.message}")
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
                gatt.disconnect()
                gatt.close()
                bluetoothGatt = null
            }
        }
    }
} 