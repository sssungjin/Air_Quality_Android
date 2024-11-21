package com.monorama.airmonomatekr.service.bluetooth

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
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

    private fun enableSensors(gatt: BluetoothGatt) {
        if (!PermissionHelper.hasBluetoothPermissions(context)) {
            return
        }

        // 센서 서비스 UUID
        val DUST_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val GAS_SERVICE_UUID = UUID.fromString("0000ffd0-0000-1000-8000-00805f9b34fb")
        val TH_SERVICE_UUID = UUID.fromString("0000ffc0-0000-1000-8000-00805f9b34fb")

        // 센서 특성 UUID
        val DUST_CHAR_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val GAS_CHAR_UUID = UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb")
        val TH_CHAR_UUID = UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb")

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 먼지 센서 활성화
            gatt.getService(DUST_SERVICE_UUID)?.let { service ->
                service.getCharacteristic(DUST_CHAR_UUID)?.let { characteristic ->
                    gatt.writeCharacteristic(characteristic, byteArrayOf(0x01),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                }
            }

            // 가스 센서 활성화
            gatt.getService(GAS_SERVICE_UUID)?.let { service ->
                service.getCharacteristic(GAS_CHAR_UUID)?.let { characteristic ->
                    gatt.writeCharacteristic(characteristic, byteArrayOf(0x01),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                }
            }

            // 온습도 센서 활성화
            gatt.getService(TH_SERVICE_UUID)?.let { service ->
                service.getCharacteristic(TH_CHAR_UUID)?.let { characteristic ->
                    gatt.writeCharacteristic(characteristic, byteArrayOf(0x02),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                }
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