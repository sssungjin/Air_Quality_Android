package com.monorama.airmonomatekr

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.monorama.airmonomatekr.ui.home.HomeScreen
import com.monorama.airmonomatekr.ui.logs.LogsScreen
import com.monorama.airmonomatekr.ui.navigation.Screen
import com.monorama.airmonomatekr.ui.settings.SettingsScreen
import com.monorama.airmonomatekr.ui.theme.AirmonomatekrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var showBluetoothDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (!allGranted) {
                showBluetoothDialog = true
            }
        }

        enableEdgeToEdge()

        setContent {
            AirmonomatekrTheme {
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
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onBluetoothPermissionNeeded = { checkAndRequestBluetoothPermissions() }
                            )
                        }
                        composable(Screen.Logs.route) { LogsScreen() }
                        composable(Screen.Settings.route) { SettingsScreen() }
                    }
                }
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        // Android 12 (API 31) 이상
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }
        // Android 10 (API 29) ~ Android 11 (API 30)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        // Android 9 (API 28) 이하
        else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("위치 권한 필요")
                .setMessage(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "블루투스 기기를 검색하고 연결하기 위해 블루투스 권한이 필요합니다."
                    } else {
                        "블루투스 기기를 검색하고 연결하기 위해 위치 권한이 필요합니다.\n" +
                        "위치 정보는 블루투스 검색 용도로만 사용됩니다."
                    }
                )
                .setPositiveButton("설정으로 이동") { _, _ ->
                    openAppSettings()
                }
                .setNegativeButton("권한 요청") { _, _ ->
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                }
                .show()
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
