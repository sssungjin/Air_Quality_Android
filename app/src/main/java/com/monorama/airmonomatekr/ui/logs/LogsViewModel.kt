package com.monorama.airmonomatekr.ui.logs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.model.DateRange
import com.monorama.airmonomatekr.data.model.Location
import com.monorama.airmonomatekr.data.model.SearchRequest
import com.monorama.airmonomatekr.data.model.SensorDataResponseDto
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

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _logs = MutableStateFlow<List<SensorLogData>>(emptyList())
    val logs: StateFlow<List<SensorLogData>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchLogs(date: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                println("LogsViewModel: Fetching logs for date: $date")
                val request = SearchRequest(
                    location = Location(),
                    dateRange = DateRange(
                        startDate = date.toString(),
                        endDate = date.toString()
                    ),
                    apiKey = Constants.API_KEY
                )
                println("LogsViewModel: Sending request: $request")

                val response = apiService.searchDevices(request)
                println("LogsViewModel: Received response: $response")
                
                // content[1]에서 실제 데이터 리스트 추출
                val dataList = (response.content[1] as? List<*>)?.filterIsInstance<Map<*, *>>()
                
                // Map을 SensorLogData로 변환
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
                            timestamp = LocalDateTime.parse(map["timestamp"] as String)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()
                        )
                    } catch (e: Exception) {
                        println("Error converting map to SensorLogData: $e")
                        null
                    }
                } ?: emptyList()
                
                println("LogsViewModel: Converted to ${_logs.value.size} SensorLogData items")
            } catch (e: Exception) {
                println("LogsViewModel: Error fetching logs - ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}