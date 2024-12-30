package com.monorama.airmonomatekr.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbDevice.getDeviceId
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.DeviceLocation
import com.monorama.airmonomatekr.data.model.Project
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.DeviceLocationRequest
import com.monorama.airmonomatekr.network.api.dto.DeviceLocationResponse
import com.monorama.airmonomatekr.network.api.dto.DeviceRegistrationRequest
import com.monorama.airmonomatekr.network.api.dto.DeviceResponseDto
import com.monorama.airmonomatekr.util.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val apiService: ApiService,
    private val workerScheduler: WorkerScheduler,
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

    private var selectedProjectId: Long? = null

    private val _deviceLocation = MutableStateFlow(DeviceLocation())
    val deviceLocation: StateFlow<DeviceLocation> = _deviceLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadDeviceInfo()
        loadProjects()
        loadDeviceLocation()
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            try {
                val response = apiService.getDevice(deviceId)
                _deviceInfo.value = response
                // 디바이스 정보로 selectedProjectId 초기화
                selectedProjectId = response.projectId

                // 서버에서 받은 정보로 로컬 설정 업데이트
                settingsDataStore.updateSettings(
                    UserSettings(
                        projectId = response.projectId?.toString() ?: "",
                        userName = response.userName,
                        email = response.userEmail,
                        transmissionMode = response.transmissionMode,
                        uploadInterval = response.uploadInterval // 기본값 설정
                    )
                )
                println("Device info loaded: $response")
            } catch (e: Exception) {
                println("Error loading device info: ${e.message}")
            }
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            try {
                val response = apiService.getProjects()
                println("SettingsViewModel: Received response: $response")

                // projects 배열에서 실제 프로젝트 데이터 추출
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

    fun setSelectedProjectId(projectId: Long) {
        selectedProjectId = projectId
    }

    fun saveSettings(
        userName: String,
        email: String,
        transmissionMode: TransmissionMode,
        minuteInterval: Int
    ) {
        viewModelScope.launch {
            try {
                // Device Registration API 호출
                val deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ) // 현재 안드로이드 디바이스의 ID 가져오기

                val request = DeviceRegistrationRequest(
                    deviceId = deviceId, // 디바이스 ID 설정
                    projectId = selectedProjectId ?: 0L, // 선택된 프로젝트 ID 설정
                    userName = userName,
                    userEmail = email, // userEmail 필드 추가
                    transmissionMode = transmissionMode,
                    uploadInterval = minuteInterval
                )

                val response = apiService.registerDevice(deviceId, request) // API 호출
                // API 호출 후 처리
                if (response.success) {
                    // 성공적으로 등록된 경우
                    println("Device registered successfully: ${response.message}")
                } else {
                    // 실패한 경우
                    println("Device registration failed: ${response.message}")
                }
            } catch (e: Exception) {
                println("Error during device registration: ${e.message}")
            }
        }
    }

    public fun loadDeviceLocation() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getDevice(deviceId)
                response.location?.let { location ->
                    _deviceLocation.value = location  // DeviceLocation 타입이 같으므로 직접 할당
                    println("Device location loaded: $location")
                } ?: run {
                    println("No location data available for device")
                    _deviceLocation.value = DeviceLocation()  // 기본값 설정
                }
            } catch (e: Exception) {
                println("Error loading device location: ${e.message}")
                e.printStackTrace()
                _deviceLocation.value = DeviceLocation()  // 에러 시 기본값 설정
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDeviceLocation(location: DeviceLocation) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val request = DeviceLocationRequest(
                    floorLevel = location.floorLevel,
                    placeType = location.placeType,
                    description = location.description
                )

                val response = apiService.updateDeviceLocation(deviceId, request)
                
                if (response.success) {
                    _deviceLocation.value = location
                    println("Device location updated successfully")
                } else {
                    _errorMessage.value = response.message
                    println("Failed to update device location: ${response.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update location: ${e.message}"
                println("Error updating device location: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}