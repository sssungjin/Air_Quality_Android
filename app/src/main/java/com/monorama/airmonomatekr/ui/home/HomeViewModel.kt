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
import com.monorama.airmonomatekr.data.repository.SensorRepository
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
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // BleManager의 sensorData를 직접 사용
    val sensorData = bleManager.sensorData

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
            _isScanning.value = false
            bleManager.stopScan()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                println("HomeViewModel: Attempting to connect to device: ${device.name}")
                // 스캔 중지
                stopScan()
                
                if (bleManager.connect(device.address)) {
                    _isConnected.value = true
                    println("HomeViewModel: Connection successful")
                    // 연결 성공 시 실시간 데이터 모니터링 시작
                    startDataMonitoring()
                } else {
                    println("HomeViewModel: Connection failed")
                    _isConnected.value = false
                }
            } catch (e: Exception) {
                println("HomeViewModel: Connection failed with error: ${e.message}")
                _isConnected.value = false
            }
        }
    }

    private fun startDataMonitoring() {
        viewModelScope.launch {
            // sensorData 상태 변경 감지
            bleManager.sensorData.collect { data ->
                // 데이터가 수신되면 UI 업데이트
                data?.let {
                    println("HomeViewModel: Received sensor data: $it")
                    // 필요한 경우 데이터 처리 및 UI 업데이트
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bleManager.disconnect()
            _isConnected.value = false
            // BleManager가 자체적으로 sensorData를 null로 초기화할 것입니다
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

    override fun onCleared() {
        super.onCleared()
        stopScan()
        bleManager.disconnect()
    }
}