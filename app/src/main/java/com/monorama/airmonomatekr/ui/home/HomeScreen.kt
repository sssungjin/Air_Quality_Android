package com.monorama.airmonomatekr.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.ui.home.components.SensorDataCard
import com.monorama.airmonomatekr.ui.theme.BlueLevel
import com.monorama.airmonomatekr.ui.theme.GreenLevel
//import com.monorama.airmonomatekr.ui.theme.OrangeLevel
import com.monorama.airmonomatekr.ui.theme.RedLevel

@Composable
fun HomeScreen(
    isConnected: Boolean = false,
    onConnectClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connection Status and Controls
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConnected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = if (isConnected) onDisconnectClick else onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Real-time Air Quality",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SensorDataCard(
            title = "Fine Dust (PM2.5)",
            value = "25",
            unit = "μg/m³",
            level = 1 // 0: Good (Blue), 1: Moderate (Green), 2: Poor (Orange), 3: Very Poor (Red)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SensorDataCard(
            title = "Carbon Dioxide (CO2)",
            value = "450",
            unit = "ppm",
            level = 0
        )

        Spacer(modifier = Modifier.height(8.dp))

        SensorDataCard(
            title = "Temperature",
            value = "24.5",
            unit = "°C",
            level = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        SensorDataCard(
            title = "Humidity",
            value = "45",
            unit = "%",
            level = 3
        )
    }
}