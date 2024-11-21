package com.monorama.airmonomatekr.data.repository

import android.bluetooth.BluetoothDevice
import com.monorama.airmonomatekr.service.bluetooth.ApiService
import com.monorama.airmonomatekr.service.bluetooth.BleManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class SensorRepository @Inject constructor(
    private val apiService: ApiService,
    private val bleManager: BleManager
) {
    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData

    suspend fun connectDevice(deviceAddress: String): Result<Unit> {
        return try {
            bleManager.connect(deviceAddress)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    suspend fun saveSensorData(data: SensorData) {
        try {
            apiService.saveSensorData(data)
        } catch (e: Exception) {
            // 에러 처리
        }
    }

    suspend fun startScan(onDevicesFound: (List<BluetoothDevice>) -> Unit) {
        bleManager.startScan(onDevicesFound)
    }

    fun stopScan() {
        bleManager.stopScan()
    }
}