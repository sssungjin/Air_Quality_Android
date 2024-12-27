package com.monorama.airmonomatekr.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.local.SettingsDataStore
import com.monorama.airmonomatekr.data.model.TransmissionMode
import com.monorama.airmonomatekr.data.model.UserSettings
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.UserLoginRequestDto
import com.monorama.airmonomatekr.network.api.dto.UserLoginResponseDto
import com.monorama.airmonomatekr.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    fun login(email: String, password: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response: UserLoginResponseDto = apiService.login(UserLoginRequestDto(email, password))
                tokenManager.saveToken(response.token) // JWT 토큰 저장

                // 사용자 정보를 SettingsDataStore에 저장
                settingsDataStore.updateSettings(
                    UserSettings(
                        userId = response.id,
                        userName = response.name,
                        email = response.email,
                        projectId = "", // 초기값 설정, 필요에 따라 수정
                        transmissionMode = TransmissionMode.REALTIME, // 초기값 설정
                        uploadInterval = 5 // 초기값 설정
                    )
                )

                onComplete()
            } catch (e: Exception) {
                println("Login failed: ${e.message}")
                onComplete()
            }
        }
    }

    fun convertToLocalDateTime(dateTimeString: String): LocalDateTime {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        return LocalDateTime.parse(dateTimeString, formatter)
    }
} 