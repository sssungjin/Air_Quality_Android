package com.monorama.airmonomatekr.data.repository

import android.bluetooth.BluetoothDevice
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.network.api.ApiService
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
    private val _sensorLogData = MutableStateFlow<SensorLogData?>(null)
    val sensorLogData: StateFlow<SensorLogData?> = _sensorLogData

    suspend fun connectDevice(deviceAddress: String): Result<Unit> {
        return try {
            bleManager.connect(deviceAddress)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    fun disconnect() {
//        bleManager.disconnect()
//    }
//
//    suspend fun startScan(onDevicesFound: (List<BluetoothDevice>) -> Unit) {
//        bleManager.startScan(onDevicesFound)
//    }
//
//    fun stopScan() {
//        bleManager.stopScan()
//    }
}