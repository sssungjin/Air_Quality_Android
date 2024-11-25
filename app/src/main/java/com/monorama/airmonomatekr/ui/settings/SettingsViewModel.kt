package com.monorama.airmonomatekr.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.Project
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.DeviceRegistrationRequest
import com.monorama.airmonomatekr.network.api.dto.DeviceResponseDto
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
    @ApplicationContext private val context: Context
) : ViewModel() {
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

    init {
        loadDeviceInfo()
        loadProjects()
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
                        transmissionMode = TransmissionMode.REALTIME
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
        transmissionMode: TransmissionMode
    ) {
        viewModelScope.launch {
            try {
                // 서버에 디바이스 정보 등록
                selectedProjectId?.let { projectId ->
                    val request = DeviceRegistrationRequest(
                        deviceId = deviceId,
                        projectId = projectId,
                        userName = userName,
                        userEmail = email
                    )

                    val response = apiService.registerDevice(deviceId, request)
                    println("Device registration response: ${response.message}")

                    if (response.success) {
                        // 성공적으로 서버에 저장되면 로컬 설정도 업데이트
                        settingsDataStore.updateSettings(
                            UserSettings(
                                projectId = projectId.toString(),
                                userName = userName,
                                email = email,
                                transmissionMode = transmissionMode
                            )
                        )
                        // 화면 갱신을 위해 디바이스 정보 다시 로드
                        loadDeviceInfo()
                    }
                }
            } catch (e: Exception) {
                println("Error saving settings: ${e.message}")
            }
        }
    }
}