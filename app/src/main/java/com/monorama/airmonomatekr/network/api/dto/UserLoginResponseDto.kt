package com.monorama.airmonomatekr.network.api.dto

import com.monorama.airmonomatekr.data.model.UserRole
import java.time.LocalDateTime

data class UserLoginResponseDto(
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole,
    val kibanaAccessKey: String?,
    val createdAt: String, // String으로 변경
    val updatedAt: String, // String으로 변경
    val token: String
)