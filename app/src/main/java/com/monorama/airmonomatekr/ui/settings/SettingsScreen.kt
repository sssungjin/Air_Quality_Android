package com.monorama.airmonomatekr.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings
import com.monorama.airmonomatekr.ui.settings.components.RegisterDeviceDialog
import com.monorama.airmonomatekr.util.Constants
import com.monorama.airmonomatekr.util.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    tokenManager: TokenManager,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showRegisterDeviceDialog by remember { mutableStateOf(false) }
    val userSettings by viewModel.userSettings.collectAsState(initial = UserSettings())
    val projects by viewModel.projects.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isDeviceRegistered by viewModel.isDeviceRegistered.collectAsState() // 디바이스 등록 상태

    // SettingsScreen.kt의 LaunchedEffect
    LaunchedEffect(Unit) {
        viewModel.loadProjects()
        viewModel.loadDeviceInfo()  // 디바이스 정보 로드
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!isDeviceRegistered) {
            // 디바이스가 등록되지 않은 경우
            Button(onClick = { showRegisterDeviceDialog = true }) {
                Text("Register Device")
            }
            Button(onClick = {
                // 로그아웃 처리
                viewModel.logout() // 로그아웃 메서드 호출
                navController.navigate("login") // 로그인 화면으로 이동
            }) {
                Text("Logout")
            }
        } else {
            // 디바이스가 등록된 경우 기존 화면 표시
            DeviceInfoScreen(
                userSettings = userSettings,
                deviceId = deviceInfo?.deviceId,
                onRegisterDeviceClick = { showRegisterDeviceDialog = true },
                onLogoutClick = {
                    viewModel.logout() // 로그아웃 메서드 호출
                    navController.navigate("login") // 로그인 화면으로 이동
                },
                selectedProject = projects.find { it.projectId == deviceInfo?.projectId } // 선택된 프로젝트 전달
            )
        }

        // Register Device Dialog
        if (showRegisterDeviceDialog) {
            RegisterDeviceDialog(
                projects = projects, // projects를 전달
                onDismiss = { showRegisterDeviceDialog = false },
                onConfirm = { projectId, transmissionMode, uploadInterval ->
                    viewModel.registerDevice(projectId, transmissionMode, uploadInterval)
                    showRegisterDeviceDialog = false
                }
            )
        }
    }
}