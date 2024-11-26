package com.monorama.airmonomatekr.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings
import com.monorama.airmonomatekr.ui.settings.components.DeviceLocationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val userSettings by viewModel.userSettings.collectAsState(initial = UserSettings())
    val projects by viewModel.projects.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    var showLocationDialog by remember { mutableStateOf(false) }
    val deviceLocation by viewModel.deviceLocation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 선택된 프로젝트 이름 초기화
    var selectedProjectName by remember(deviceInfo, projects) {
        mutableStateOf(
            projects.find { it.projectId == deviceInfo?.projectId }?.projectName ?: ""
        )
    }

    // userName과 email 초기화
    var userName by remember(deviceInfo) {
        mutableStateOf(deviceInfo?.userName ?: userSettings.userName)
    }
    var email by remember(deviceInfo) {
        mutableStateOf(deviceInfo?.userEmail ?: userSettings.email)
    }
    var transmissionMode by remember { mutableStateOf(userSettings.transmissionMode) }
    var expanded by remember { mutableStateOf(false) }


    // SettingsScreen.kt의 LaunchedEffect
    LaunchedEffect(Unit) {
        viewModel.loadProjects()
        viewModel.loadDeviceLocation()  // 화면 진입 시에도 위치 정보 로드
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Project Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (isEditing) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedProjectName,
                onValueChange = {},
                readOnly = true,
                enabled = isEditing,
                label = { Text("Project") },
                trailingIcon = {
                    if (isEditing) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            if (isEditing && projects.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text(project.projectName) },
                            onClick = {
                                selectedProjectName = project.projectName
                                viewModel.setSelectedProjectId(project.projectId)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // User Name
        OutlinedTextField(
            value = userName,
            onValueChange = { if (isEditing) userName = it },
            label = { Text("User Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { if (isEditing) email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )

        // Transmission Mode
        Text(
            text = "Data Transmission Mode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        TransmissionMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = transmissionMode == mode,
                        enabled = isEditing,
                        onClick = { if (isEditing) transmissionMode = mode }
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = transmissionMode == mode,
                    onClick = { if (isEditing) transmissionMode = mode },
                    enabled = isEditing
                )
                Text(
                    text = when (mode) {
                        TransmissionMode.REALTIME -> "Real-time"
                        TransmissionMode.MINUTE -> "Every Minute"
                        TransmissionMode.DAILY -> "Daily"
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Location Button
        Button(
            onClick = {
                viewModel.loadDeviceLocation()  // 다이얼로그를 열기 전에 위치 정보 로드
                showLocationDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Device Location")
        }

        // Location Dialog
        if (showLocationDialog) {
            DeviceLocationDialog(
                currentLocation = deviceLocation,
                isLoading = isLoading,
                onDismiss = { showLocationDialog = false },
                onConfirm = { location ->
                    viewModel.updateDeviceLocation(location)
                    showLocationDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isEditing) {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
            } else {
                Button(
                    onClick = {
                        viewModel.saveSettings(
                            userName = userName,
                            email = email,
                            transmissionMode = transmissionMode
                        )
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = {
                        isEditing = false
                        userName = userSettings.userName
                        email = userSettings.email
                        transmissionMode = userSettings.transmissionMode
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}