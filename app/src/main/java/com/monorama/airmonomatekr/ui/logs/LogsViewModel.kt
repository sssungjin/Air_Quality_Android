package com.monorama.airmonomatekr.ui.logs

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.model.DateRange
import com.monorama.airmonomatekr.data.model.Location
import com.monorama.airmonomatekr.data.model.SearchRequest
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.util.Constants
import com.monorama.airmonomatekr.network.api.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import com.google.gson.Gson

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _logs = MutableStateFlow<List<SensorLogData>>(emptyList())
    val logs: StateFlow<List<SensorLogData>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private var selectedDate: LocalDate? = null
    private val pageSize = 20

    fun setPage(page: Int) {
        viewModelScope.launch {
            _currentPage.value = page
            selectedDate?.let { fetchLogs(it, page) }
        }
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    fun fetchLogs(date: LocalDate, page: Int = 0) {
        viewModelScope.launch {
            _isLoading.value = true
            selectedDate = date
            try {
                val startDateTime = date.atStartOfDay()
                val endDateTime = date.atTime(23, 59, 59)

                val response = apiService.getDeviceSensorHistory(
                    deviceId = getDeviceId(),
                    startDate = startDateTime.toString(),
                    endDate = endDateTime.toString(),
                    page = page,
                    size = pageSize
                )

                // content[1]이 실제 데이터 리스트를 포함하고 있다고 가정
                val dataList = (response.content[1] as? List<*>)?.filterIsInstance<Map<*, *>>()

                _logs.value = dataList?.mapNotNull { map ->
                    try {
                        SensorLogData(
                            pm25 = SensorLogData.SensorValue(
                                value = (map["pm25Value"] as? Double)?.toFloat() ?: 0f,
                                level = (map["pm25Level"] as? Double)?.toInt() ?: 0
                            ),
                            pm10 = SensorLogData.SensorValue(
                                value = (map["pm10Value"] as? Double)?.toFloat() ?: 0f,
                                level = (map["pm10Level"] as? Double)?.toInt() ?: 0
                            ),
                            temperature = SensorLogData.SensorValue(
                                value = (map["temperature"] as? Double)?.toFloat() ?: 0f,
                                level = (map["temperatureLevel"] as? Double)?.toInt() ?: 0
                            ),
                            humidity = SensorLogData.SensorValue(
                                value = (map["humidity"] as? Double)?.toFloat() ?: 0f,
                                level = (map["humidityLevel"] as? Double)?.toInt() ?: 0
                            ),
                            co2 = SensorLogData.SensorValue(
                                value = (map["co2Value"] as? Double)?.toFloat() ?: 0f,
                                level = (map["co2Level"] as? Double)?.toInt() ?: 0
                            ),
                            voc = SensorLogData.SensorValue(
                                value = (map["vocValue"] as? Double)?.toFloat() ?: 0f,
                                level = (map["vocLevel"] as? Double)?.toInt() ?: 0
                            ),
                            timestampStr = map["timestamp"] as String,
                            timestamp = LocalDateTime.parse(map["timestamp"] as String)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()
                        )
                    } catch (e: Exception) {
                        println("Error converting map to SensorLogData: $e")
                        null
                    }
                } ?: emptyList()

                _totalPages.value = if (response.totalPages > 0) response.totalPages else 1
                _currentPage.value = response.number.coerceIn(0, _totalPages.value - 1)
            } catch (e: Exception) {
                println("LogsViewModel: Error fetching logs - ${e.message}")
                e.printStackTrace()
                _logs.value = emptyList()
                _totalPages.value = 1
                _currentPage.value = 0
            } finally {
                _isLoading.value = false
            }
        }
    }
}