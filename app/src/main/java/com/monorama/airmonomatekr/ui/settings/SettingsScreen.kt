package com.monorama.airmonomatekr.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings
import com.monorama.airmonomatekr.ui.settings.components.DeviceLocationDialog
import com.monorama.airmonomatekr.util.Constants
import com.monorama.airmonomatekr.util.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    tokenManager: TokenManager,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val userSettings by viewModel.userSettings.collectAsState(initial = UserSettings())
    val projects by viewModel.projects.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    var showLocationDialog by remember { mutableStateOf(false) }
    val deviceLocation by viewModel.deviceLocation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showWebView by remember { mutableStateOf(false) }

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
    var transmissionMode by remember(deviceInfo, userSettings) {
        mutableStateOf(deviceInfo?.transmissionMode ?: userSettings.transmissionMode)
    }
    
    // minuteInterval을 올바르게 설정
    var minuteInterval by remember(deviceInfo, userSettings) {
        mutableStateOf(deviceInfo?.uploadInterval?.toString() ?: userSettings.uploadInterval.toString())
    }

    val context = LocalContext.current

    val deviceId = deviceInfo?.deviceId

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
            expanded = false,
            onExpandedChange = {}
        ) {
            OutlinedTextField(
                value = selectedProjectName,
                onValueChange = {},
                readOnly = true,
                enabled = isEditing,
                label = { Text("Project") },
                trailingIcon = {
                    if (isEditing) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
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

        Spacer(modifier = Modifier.height(8.dp))

        // Transmission Mode Radio Buttons
        Text("Transmission Mode", style = MaterialTheme.typography.titleMedium)
        TransmissionMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (transmissionMode == mode),
                        onClick = { if (isEditing) transmissionMode = mode }
                    ),
                verticalAlignment = Alignment.CenterVertically // 아이템 정렬
            ) {
                RadioButton(
                    selected = (transmissionMode == mode),
                    onClick = null, // `Row`의 `onClick`에서 처리하므로 null
                    enabled = isEditing
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

        // 분 단위 입력 필드 추가
        OutlinedTextField(
            value = minuteInterval,
            onValueChange = { minuteInterval = it },
            label = { Text("Upload Interval (minutes)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            enabled = isEditing // 편집 가능 상태에서만 활성화
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                            transmissionMode = transmissionMode,
                            minuteInterval = minuteInterval.toIntOrNull() ?: 5 // 기본 5분
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

        Spacer(modifier = Modifier.height(16.dp))

        // Device Location Button - Modified to open web link
        Button(
            onClick = {
                val url = buildString {
                    append(Constants.WebUrl.WEB_URL)
                    append("/device-info")
                    append("?deviceId=$deviceId")
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = deviceId != null
        ) {
            Text("Visit&Edit Device Info Page")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Project Info Button
        Button(
            onClick = {
                val projectId = deviceInfo?.projectId ?: return@Button
                val url = buildString {
                    append(
                        Constants.WebUrl.WEB_URL + "/info")
                    append("?deviceId=$deviceId")
                    append("&projectId=$projectId")
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = deviceInfo?.projectId != null
        ) {
            Text("Visit&Edit Project Info Page")
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

        // 로그아웃 버튼 추가
        Button(
            onClick = {
                tokenManager.clearToken() // 토큰 제거
                navController.navigate("login") // 로그인 화면으로 이동
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}