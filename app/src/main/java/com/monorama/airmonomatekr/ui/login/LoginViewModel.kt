package com.monorama.airmonomatekr.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.UserLoginRequestDto
import com.monorama.airmonomatekr.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    fun login(email: String, password: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.login(UserLoginRequestDto(email, password))
                tokenManager.saveToken(response.token) // JWT 토큰 저장
                onComplete()
            } catch (e: Exception) {
                println("Login failed: ${e.message}")
                onComplete()
            }
        }
    }
} 