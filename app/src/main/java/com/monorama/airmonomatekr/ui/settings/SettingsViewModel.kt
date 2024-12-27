package com.monorama.airmonomatekr.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.DeviceLocation
import com.monorama.airmonomatekr.data.model.Project
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.DeviceRegistrationRequest
import com.monorama.airmonomatekr.network.api.dto.DeviceResponseDto
import com.monorama.airmonomatekr.network.api.dto.UserResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import com.monorama.airmonomatekr.util.TokenManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    @SuppressLint("HardwareIds")
    private val deviceId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    val userSettings = settingsDataStore.userSettings

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceResponseDto?>(null)
    val deviceInfo: StateFlow<DeviceResponseDto?> = _deviceInfo.asStateFlow()

    private val _deviceLocation = MutableStateFlow(DeviceLocation())
    val deviceLocation: StateFlow<DeviceLocation> = _deviceLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDeviceRegistered = MutableStateFlow(false)
    val isDeviceRegistered: StateFlow<Boolean> = _isDeviceRegistered.asStateFlow()

    init {
        loadProjects()
        loadDeviceInfo()
        //loadDeviceLocation()
    }

//    fun loadDeviceInfo() {
//        viewModelScope.launch {
//            try {
//                val response = apiService.getDevice(deviceId)
//                _deviceInfo.value = response
//                _isDeviceRegistered.value = true
//
//                // userId를 가져오기 위해 userSettings를 collect
//                userSettings.collect { settings ->
//                    val userId = settings.userId
//                    loadUserInfo(userId)
//                }
//
//                println("Device info loaded: $response")
//            } catch (e: Exception) {
//                if (e.message?.contains("410") == true) {
//                    _isDeviceRegistered.value = false
//                    println("Device not registered: ${e.message}")
//                } else {
//                    println("Error loading device info: ${e.message}")
//                }
//            }
//        }
//    }

    private fun loadUserInfo(userId: Long) {
        viewModelScope.launch {
            try {
                val userResponse: UserResponseDto = apiService.getUserById(userId)
                settingsDataStore.updateSettings(
                    UserSettings(
                        userId = userResponse.id,
                        projectId = _deviceInfo.value?.projectId?.toString() ?: "",
                        userName = userResponse.name,
                        email = userResponse.email,
                        transmissionMode = _deviceInfo.value?.transmissionMode ?: TransmissionMode.REALTIME,
                        uploadInterval = _deviceInfo.value?.uploadInterval ?: 5
                    )
                )
                println("User info loaded: ${userResponse.name}, ${userResponse.email}")
            } catch (e: Exception) {
                println("Error loading user info: ${e.message}")
            }
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            try {
                val response = apiService.getProjects()
                println("SettingsViewModel: Received response: $response")

                // projects 배열에서 두 번째 요소를 가져와서 List<Project>로 변환
                val projectsList = (response.projects[1] as? List<*>)?.filterIsInstance<Map<*, *>>()

                _projects.value = projectsList?.mapNotNull { map ->
                    try {
                        Project(
                            projectId = (map["projectId"] as? Double)?.toLong() ?: 0L,
                            projectName = (map["projectName"] as? String) ?: "",
                            description = (map["description"] as? String) ?: "",
                            createdAt = (map["createdAt"] as? String) ?: ""
                        )
                    } catch (e: Exception) {
                        println("Error converting map to Project: $e")
                        null
                    }
                } ?: emptyList()

                println("SettingsViewModel: Converted to ${_projects.value.size} Project items")
                _projects.value.forEach { project ->
                    println("Project: ${project.projectName} (ID: ${project.projectId})")
                }
            } catch (e: Exception) {
                println("SettingsViewModel: Error loading projects - ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun loadDeviceInfo() {
        viewModelScope.launch {
            try {
                val response = apiService.getDevice(deviceId)
                _deviceInfo.value = response
                _isDeviceRegistered.value = true

                // userId를 가져오기 위해 userSettings를 collect
                userSettings.collect { settings ->
                    val userId = settings.userId
                    loadUserInfo(userId)
                }

                // 프로젝트 정보 가져오기
                loadProjects() // 프로젝트를 먼저 가져옵니다.

                println("Device info loaded: $response")
            } catch (e: Exception) {
                if (e.message?.contains("410") == true) {
                    _isDeviceRegistered.value = false
                    println("Device not registered: ${e.message}")
                } else {
                    println("Error loading device info: ${e.message}")
                }
            }
        }
    }


    fun loadDeviceLocation() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getDevice(deviceId)
                _deviceLocation.value = response.location ?: DeviceLocation()
                println("Device location loaded: ${_deviceLocation.value}")
            } catch (e: Exception) {
                println("Error loading device location: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerDevice(projectId: Long, transmissionMode: TransmissionMode, uploadInterval: Int) {
        viewModelScope.launch {
            try {
                val request = DeviceRegistrationRequest(
                    deviceId = deviceId,
                    projectId = projectId,
                    transmissionMode = transmissionMode,
                    uploadInterval = uploadInterval
                )
                val response = apiService.registerDevice(deviceId, request)
                if (response.success) {
                    println("Device registered successfully")
                    _isDeviceRegistered.value = true
                } else {
                    println("Failed to register device: ${response.message}")
                }
            } catch (e: Exception) {
                println("Error registering device: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
        }
    }
}