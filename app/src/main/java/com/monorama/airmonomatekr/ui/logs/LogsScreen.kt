package com.monorama.airmonomatekr.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monorama.airmonomatekr.ui.logs.components.DatePickerComponent
import com.monorama.airmonomatekr.ui.logs.components.LogItem
import java.time.LocalDate

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val logs by viewModel.logs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()

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
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pageSize = 10
                val startIndex = currentPage * pageSize
                val endIndex = minOf(startIndex + pageSize, logs.size)
                val currentPageItems = logs.subList(startIndex, endIndex)

                items(currentPageItems) { log ->
                    LogItem(log)
                }
            }
        }

        // 페이지네이션 컨트롤
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 처음으로
            TextButton(
                onClick = { viewModel.setPage(0) },
                enabled = currentPage > 0
            ) {
                Text("<<")
            }

            // 이전 10페이지
            TextButton(
                onClick = { viewModel.setPage(maxOf(0, currentPage - 10)) },
                enabled = currentPage >= 10
            ) {
                Text("<")
            }

            // 페이지 번호들
            val startPage = (currentPage / 10) * 10
            val endPage = minOf(startPage + 9, totalPages - 1)
            
            (startPage..endPage).forEach { page ->
                TextButton(
                    onClick = { viewModel.setPage(page) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (page == currentPage) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = (page + 1).toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (page == currentPage) 
                                FontWeight.Bold 
                            else 
                                FontWeight.Normal
                        )
                    )
                }
            }

            // 다음 10페이지
            TextButton(
                onClick = { viewModel.setPage(minOf(totalPages - 1, currentPage + 10)) },
                enabled = currentPage < totalPages - 10
            ) {
                Text(">")
            }

            // 마지막으로
            TextButton(
                onClick = { viewModel.setPage(totalPages - 1) },
                enabled = currentPage < totalPages - 1
            ) {
                Text(">>")
            }
        }
    }
}