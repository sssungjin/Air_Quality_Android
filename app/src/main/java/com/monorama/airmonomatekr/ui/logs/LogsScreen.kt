package com.monorama.airmonomatekr.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

    Box(modifier = Modifier.fillMaxSize()) {
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
            Box(
                modifier = Modifier.weight(1f),
            ) {
                if (logs.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("데이터가 없습니다")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { log ->
                            LogItem(log)
                        }
                    }
                }
            }

            // 페이지네이션 컨트롤
            if (totalPages > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 버튼 내용 수정
                    TextButton(
                        onClick = { viewModel.setPage(0) },
                        enabled = currentPage > 0,
                        modifier = Modifier
                            .weight(1f) // 동적으로 크기 조정
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp) // 패딩 조정
                    ) {
                        Text("<<", style = MaterialTheme.typography.bodySmall)
                    }

                    TextButton(
                        onClick = { viewModel.setPage(currentPage - 1) },
                        enabled = currentPage > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("<", style = MaterialTheme.typography.bodySmall)
                    }

                    val pageNumbers = when {
                        totalPages <= 5 -> 0 until totalPages
                        currentPage <= 2 -> 0..4
                        currentPage >= totalPages - 3 -> (totalPages - 5) until totalPages
                        else -> (currentPage - 2)..(currentPage + 2)
                    }

                    pageNumbers.forEach { page ->
                        TextButton(
                            onClick = { viewModel.setPage(page) },
                            modifier = Modifier
                                .weight(1f) // 동적으로 크기 조정
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (page == currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = (page + 1).toString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (page == currentPage)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal
                                )
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.setPage(currentPage + 1) },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(">", style = MaterialTheme.typography.bodySmall)
                    }

                    TextButton(
                        onClick = { viewModel.setPage(totalPages - 1) },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(">>", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // 로딩 인디케이터를 화면 정중앙에 표시
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}