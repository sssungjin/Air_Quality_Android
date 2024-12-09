package com.monorama.airmonomatekr.ui.logs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.data.model.SensorLogData
import com.monorama.airmonomatekr.ui.theme.BlueLevel
import com.monorama.airmonomatekr.ui.theme.GreenLevel
import com.monorama.airmonomatekr.ui.theme.OrangeLevel
import com.monorama.airmonomatekr.ui.theme.RedLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogItem(log: SensorLogData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = log.timestampStr?.replace("T", " ") ?: "",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SensorValueItem("PM2.5", log.pm25.value, log.pm25.level, "μg/m³")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SensorValueItem("PM10", log.pm10.value, log.pm10.level, "μg/m³")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SensorValueItem("Temperature", log.temperature.value, log.temperature.level, "°C")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SensorValueItem("Humidity", log.humidity.value, log.humidity.level, "%")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SensorValueItem("CO2", log.co2.value, log.co2.level, "ppm")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SensorValueItem("VOC", log.voc.value, log.voc.level, "ppb")
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorValueItem(label: String, value: Float, level: Int, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when (level) {
                        0 -> BlueLevel
                        1 -> GreenLevel
                        2 -> OrangeLevel
                        3 -> RedLevel
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
        )
    }
}

@Composable
private fun SensorLevelIndicator(level: Int) {
    val color = when (level) {
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.tertiary
        3 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}