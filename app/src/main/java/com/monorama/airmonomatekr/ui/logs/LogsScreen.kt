package com.monorama.airmonomatekr.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.ui.logs.components.LogItem

@Composable
fun LogsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Data Logs",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sample data
        val logs = remember {
            List(10) { index ->
                "Log ${index + 1}" to "2024-03-${20 + index} 14:${30 + index}:00"
            }
        }

        LazyColumn {
            items(logs) { (message, timestamp) ->
                LogItem(message = message, timestamp = timestamp)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
} 