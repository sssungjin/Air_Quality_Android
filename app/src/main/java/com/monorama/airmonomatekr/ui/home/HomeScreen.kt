package com.monorama.airmonomatekr.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.monorama.airmonomatekr.ui.home.components.SensorDataCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>()
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    var showDeviceList by remember { mutableStateOf(false) }

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
                    onClick = {
                        if (isConnected) {
                            viewModel.disconnect()
                        } else {
                            showDeviceList = true
                            viewModel.startScan()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect")
                }
            }
        }

        // 블루투스 디바이스 목록 다이얼로그
        if (showDeviceList) {
            AlertDialog(
                onDismissRequest = {
                    showDeviceList = false
                    viewModel.stopScan()
                },
                title = { Text("Available Devices") },
                text = {
                    Column {
                        if (isScanning) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        discoveredDevices.forEach { device ->
                            TextButton(
                                onClick = {
                                    viewModel.connectToDevice(device)
                                    showDeviceList = false
                                    viewModel.stopScan()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    Text(device.name ?: "Unknown Device")
                                } else {
                                    Text("Unknown Device")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeviceList = false
                        viewModel.stopScan()
                    }) {
                        Text("Cancel")
                    }
                }
            )
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