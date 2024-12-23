package com.monorama.airmonomatekr.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monorama.airmonomatekr.data.model.UserRole
import com.monorama.airmonomatekr.network.api.ApiService
import com.monorama.airmonomatekr.network.api.dto.UserRegistrationRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    fun register(email: String, password: String, name: String, role: UserRole, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.registerUser(UserRegistrationRequestDto(email, password, name, role))
                onComplete(true, null)
            } catch (e: Exception) {
                println("Registration failed: ${e.message}")
                onComplete(false, e.message)
            }
        }
    }
} 