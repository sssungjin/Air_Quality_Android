package com.monorama.airmonomatekr.ui.logs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.model.DateRange
import com.monorama.airmonomatekr.data.model.SearchRequest
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
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
                val response = apiService.searchDevices(
                    SearchRequest(
                        dateRange = DateRange(
                            startDate = date.toString(),
                            endDate = date.toString()
                        ),
                        apiKey = Constants.API_KEY
                    )
                )
                _logs.value = response.content
                println("LogsViewModel: Fetched ${response.content.size} logs for date: $date")
            } catch (e: Exception) {
                println("LogsViewModel: Error fetching logs: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}