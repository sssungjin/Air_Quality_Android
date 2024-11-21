package com.monorama.airmonomatekr.ui.home

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _isScanning = MutableStateFlow<Boolean>(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isConnected = MutableStateFlow<Boolean>(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            if (!isBluetoothEnabled()) {
                // 블루투스가 비활성화된 경우 처리
                return@launch
            }
            _isScanning.value = true
            sensorRepository.startScan { devices ->
                _discoveredDevices.value = devices
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            _isScanning.value = false
            sensorRepository.stopScan()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            sensorRepository.connectDevice(device.address)
                .onSuccess {
                    _isConnected.value = true
                }
                .onFailure {
                    // 에러 처리
                }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            sensorRepository.disconnect()
            _isConnected.value = false
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled == true
    }
} 