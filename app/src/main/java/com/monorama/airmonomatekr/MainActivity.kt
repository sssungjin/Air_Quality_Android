package com.monorama.airmonomatekr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.monorama.airmonomatekr.ui.home.HomeScreen
import com.monorama.airmonomatekr.ui.logs.LogsScreen
import com.monorama.airmonomatekr.ui.navigation.Screen
import com.monorama.airmonomatekr.ui.settings.SettingsScreen
import com.monorama.airmonomatekr.ui.theme.AirmonomatekrTheme
import com.monorama.airmonomatekr.util.PermissionHelper
import com.monorama.airmonomatekr.ui.components.PermissionDialog
import androidx.compose.runtime.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ActivityResultLauncher 초기화
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 권한 요청 런처 초기화
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            handlePermissionResult(allGranted)
        }

        // Edge-to-edge 활성화
        enableEdgeToEdge()

        setContent {
            AirmonomatekrTheme {
                // Compose에서 상태를 remember로 관리
                var showPermissionDialog by remember { mutableStateOf(false) }
                var permissionText by remember { mutableStateOf("") }
                var isPermanentlyDeclined by remember { mutableStateOf(false) }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Real-time") },
                                selected = currentRoute == Screen.Home.route,
                                onClick = { navController.navigate(Screen.Home.route) }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.List, contentDescription = "Logs") },
                                label = { Text("Logs") },
                                selected = currentRoute == Screen.Logs.route,
                                onClick = { navController.navigate(Screen.Logs.route) }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentRoute == Screen.Settings.route,
                                onClick = { navController.navigate(Screen.Settings.route) }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) { HomeScreen() }
                        composable(Screen.Logs.route) { LogsScreen() }
                        composable(Screen.Settings.route) { SettingsScreen() }
                    }
                }

                // 권한 요청 다이얼로그 표시
                if (showPermissionDialog) {
                    PermissionDialog(
                        permissionTextProvider = permissionText,
                        isPermanentlyDeclined = isPermanentlyDeclined,
                        onDismiss = { showPermissionDialog = false },
                        onOkClick = {
                            showPermissionDialog = false
                            checkAndRequestPermissions()
                        },
                        onGoToAppSettingsClick = { openAppSettings() }
                    )
                }
            }
        }

        // 권한 요청 시작
        checkAndRequestPermissions()
    }

    private fun handlePermissionResult(allGranted: Boolean) {
        if (!allGranted) {
            val isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                PermissionHelper.requiredPermissions.first()
            )
            if (isPermanentlyDeclined) {
                // 사용자가 "다시 묻지 않음"을 선택한 경우 설정으로 이동
                openAppSettings()
            } else {
                // 권한을 다시 요청
                permissionLauncher.launch(PermissionHelper.requiredPermissions)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionHelper.hasRequiredPermissions(this)) {
            permissionLauncher.launch(PermissionHelper.requiredPermissions)
        }
    }

    private fun openAppSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also { intent ->
            startActivity(intent)
        }
    }
}
