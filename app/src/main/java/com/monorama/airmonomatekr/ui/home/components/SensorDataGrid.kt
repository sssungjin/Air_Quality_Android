package com.monorama.airmonomatekr.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.data.model.SensorLogData

@Composable
fun SensorDataGrid(sensorData: SensorLogData) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            SensorDataCard(
                title = "PM2.5",
                value = sensorData.pm25.value.toString(),
                unit = "μg/m³",
                level = sensorData.pm25.level
            )
        }
        item {
            SensorDataCard(
                title = "PM10",
                value = sensorData.pm10.value.toString(),
                unit = "μg/m³",
                level = sensorData.pm10.level
            )
        }
        item {
            SensorDataCard(
                title = "Temperature",
                value = sensorData.temperature.value.toString(),
                unit = "°C",
                level = sensorData.temperature.level
            )
        }
        item {
            SensorDataCard(
                title = "Humidity",
                value = sensorData.humidity.value.toString(),
                unit = "%",
                level = sensorData.humidity.level
            )
        }
        item {
            SensorDataCard(
                title = "CO2",
                value = sensorData.co2.value.toString(),
                unit = "ppm",
                level = sensorData.co2.level
            )
        }
        item {
            SensorDataCard(
                title = "VOC",
                value = sensorData.voc.value.toString(),
                unit = "ppb",
                level = sensorData.voc.level
            )
        }
    }
}