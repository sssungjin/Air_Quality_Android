package com.monorama.airmonomatekr.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.data.model.Project
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    userSettings: UserSettings,
    deviceId: String?,
    onRegisterDeviceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    selectedProject: Project? // 선택된 프로젝트를 전달
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 선택된 프로젝트 이름 표시
        Text(
            text = selectedProject?.projectName ?: "No Project Selected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // User Name
        OutlinedTextField(
            value = userSettings.userName,
            onValueChange = {},
            label = { Text("User Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false // 비활성화
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email
        OutlinedTextField(
            value = userSettings.email,
            onValueChange = {},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false // 비활성화
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Transmission Mode Radio Buttons
        Text("Transmission Mode", style = MaterialTheme.typography.titleMedium)
        TransmissionMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (userSettings.transmissionMode == mode),
                        onClick = {}
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (userSettings.transmissionMode == mode),
                    onClick = null
                )
                Text(
                    text = when (mode) {
                        TransmissionMode.REALTIME -> "Real-time"
                        TransmissionMode.MINUTE -> "Minute"
                        TransmissionMode.DAILY -> "Daily"
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upload Interval
        OutlinedTextField(
            value = userSettings.uploadInterval.toString(),
            onValueChange = {},
            label = { Text("Upload Interval (minutes)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            enabled = false // 비활성화
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Register Device Button
        if (deviceId != null) {
            Button(onClick = onRegisterDeviceClick) {
                Text("Register Device")
            }
        }

        Button(onClick = onLogoutClick) {
            Text("Logout")
        }
    }
}
