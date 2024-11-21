package com.monorama.airmonomatekr.ui.home

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sensorRepository: SensorRepository
) : ViewModel() {
    private val _isScanning = MutableStateFlow<Boolean>(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isConnected = MutableStateFlow<Boolean>(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            sensorRepository.startScan { devices: List<BluetoothDevice> ->
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
} 