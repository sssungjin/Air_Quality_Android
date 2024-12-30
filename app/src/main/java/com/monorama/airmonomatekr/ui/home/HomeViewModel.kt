package com.monorama.airmonomatekr.ui.home

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.network.websocket.WebSocketManager
import com.monorama.airmonomatekr.service.bluetooth.BleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val webSocketManager: WebSocketManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isCollectingData = MutableStateFlow(false)
    val isCollectingData: StateFlow<Boolean> = _isCollectingData.asStateFlow()

    val sensorData = bleManager.sensorLogData

    val webSocketConnected = webSocketManager.isConnected.value

    fun startScan() {
        viewModelScope.launch {
            if (!isBluetoothEnabled()) {
                println("HomeViewModel: Bluetooth not enabled")
                return@launch
            }
            _isScanning.value = true
            println("HomeViewModel: Starting scan...")
            bleManager.startScan { devices ->
                println("HomeViewModel: Received ${devices.size} devices")
                _discoveredDevices.value = devices
                println("HomeViewModel: Updated discoveredDevices with ${_discoveredDevices.value?.size} devices")
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            println("HomeViewModel: Stopping scan")
            _isScanning.value = false
            bleManager.stopScan()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
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
                println("HomeViewModel: Attempting to connect to device: ${device.name}")
                stopScan() // 스캔 중지

                // BluetoothDevice 객체를 사용하여 연결
                if (bleManager.connect(device)) {
                    _isConnected.value = true
                    _isCollectingData.value = true
                    println("HomeViewModel: Connection successful")
                } else {
                    _isConnected.value = false
                    _isCollectingData.value = false
                    println("HomeViewModel: Connection failed")
                }
            } catch (e: Exception) {
                println("HomeViewModel: Connection failed with error: ${e.message}")
                _isConnected.value = false
                _isCollectingData.value = false
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                println("HomeViewModel: Starting disconnect process...")

                // 1. 스캔 중지
                stopScan()

                // 2.1. BLE 연결 해제
                bleManager.disconnect()

                // 2.2. 웹소켓 연결 해제
                webSocketManager.disconnect()

                // 3. 상태 초기화
                _isConnected.value = false
                _isCollectingData.value = false
                _discoveredDevices.value = emptyList()

                // 4. 블루투스 관련 상태 재설정
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothManager.adapter?.cancelDiscovery()
                }

                println("HomeViewModel: Disconnect process completed")
            } catch (e: Exception) {
                println("HomeViewModel: Error during disconnect: ${e.message}")
                // 에러가 발생하더라도 상태는 초기화
                _isConnected.value = false
                _isCollectingData.value = false
                e.printStackTrace()
            }
        }
    }

    fun onMenuNavigated() {
        // 메뉴로 이동할 때 연결 상태 유지
        // UI 업데이트를 위한 상태 설정
        if (_isConnected.value) {
            println("HomeViewModel: Connection is still active.")
            // UI 업데이트를 위한 추가 로직이 필요할 수 있습니다.
        } else {
            println("HomeViewModel: Connection is not active.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                disconnect()
                // ViewModel이 제거될 때 모든 리소스 정리
                bleManager.disconnect()
            } catch (e: Exception) {
                println("HomeViewModel: Error during cleanup: ${e.message}")
            }
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null) {
            println("HomeViewModel: Device doesn't support Bluetooth")
            return false
        }

        if (!adapter.isEnabled) {
            println("HomeViewModel: Bluetooth is not enabled")
            return false
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            println("HomeViewModel: Location service is not enabled")
            return false
        }

        return true
    }

    fun checkConnectionStatus() {
        viewModelScope.launch {
            // BLE 연결 상태 확인
            _isConnected.value = bleManager.isConnected.value
            println("HomeViewModel: BLE Connected: ${_isConnected.value}")

            // WebSocket 연결 상태 확인
            _isCollectingData.value = webSocketManager.isConnected.value
            println("HomeViewModel: WebSocket Connected: ${_isCollectingData.value}")
        }
    }

}