package com.monorama.airmonomatekr.network.api.dto

import com.monorama.airmonomatekr.data.model.UserRole

data class UserRegistrationRequestDto(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole // UserRole은 enum으로 정의되어 있어야 합니다.
) 