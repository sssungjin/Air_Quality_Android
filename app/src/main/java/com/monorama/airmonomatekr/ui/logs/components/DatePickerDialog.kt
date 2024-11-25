package com.monorama.airmonomatekr.ui.logs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerComponent(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        ?: "Select Date",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date"
                )
            }
        }
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            ),
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedLocalDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedLocalDate)
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}