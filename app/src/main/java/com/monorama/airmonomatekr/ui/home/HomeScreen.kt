package com.monorama.airmonomatekr.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.monorama.airmonomatekr.ui.home.components.SensorDataCard
import com.monorama.airmonomatekr.ui.home.components.SensorDataGrid
import com.monorama.airmonomatekr.util.PermissionHelper

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onBluetoothPermissionNeeded: () -> Unit = {},
    onEnableBluetoothRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    var showDeviceList by remember { mutableStateOf(false) }

    // 센서 데이터 상태 추가
    val sensorData by viewModel.sensorData.collectAsState()

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
                            // 블루투스 권한이 없는 경우에만 권한 요청
                            if (!PermissionHelper.hasRequiredPermissions(context)) {
                                onBluetoothPermissionNeeded()
                            } else {
                                showDeviceList = true
                                viewModel.startScan()
                            }
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
                title = { Text("Available Devices (${discoveredDevices.size})") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 스캔 중 표시
                        if (isScanning) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = if (discoveredDevices.isEmpty())
                                    "Scanning for devices..."
                                else
                                    "Found ${discoveredDevices.size} device(s)",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // 발견된 디바이스 목록
                        discoveredDevices.forEach { device ->
                            Button(
                                onClick = {
                                    if (ActivityCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        // TODO: Consider calling
                                        //    ActivityCompat#requestPermissions
                                        // here to request the missing permissions, and then overriding
                                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                        //                                          int[] grantResults)
                                        // to handle the case where the user grants the permission. See the documentation
                                        // for ActivityCompat#requestPermissions for more details.
                                    }
                                    println("HomeScreen: Device selected: ${device.name}")
                                    viewModel.connectToDevice(device)
                                    showDeviceList = false  // 다이얼로그 닫기
                                    viewModel.stopScan()    // 스캔 중지
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = device.name ?: "Unknown Device",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // 디바이스를 찾지 못했을 때 메시지
                        if (discoveredDevices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isScanning)
                                        "Searching for devices..."
                                    else
                                        "No devices found.\nMake sure your device is nearby and discoverable.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isScanning) {
                                viewModel.stopScan()
                            } else {
                                viewModel.startScan()
                            }
                        }
                    ) {
                        Text(if (isScanning) "Stop Scan" else "Rescan")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeviceList = false
                            viewModel.stopScan()
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isConnected) {
            Text(
                text = "Real-time Air Quality",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            sensorData?.let { data ->
                SensorDataGrid(data)
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Waiting for sensor data...",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 연결되지 않은 상태일 때 메시지 표시
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connect to view real-time air quality data",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Press the Connect button to start scanning for devices",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}