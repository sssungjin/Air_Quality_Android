package com.monorama.airmonomatekr.ui.logs

import DatePickerComponent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monorama.airmonomatekr.ui.logs.components.LogItem
import java.time.LocalDate

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val logs by viewModel.logs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 날짜 선택기
        DatePickerComponent(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                viewModel.fetchLogs(date)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 로그 데이터 표시
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn {
                items(logs) { log ->
                    LogItem(log)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}