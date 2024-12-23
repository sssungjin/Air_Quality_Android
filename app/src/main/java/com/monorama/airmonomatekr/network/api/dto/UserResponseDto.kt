package com.monorama.airmonomatekr.network.api.dto

import com.monorama.airmonomatekr.data.model.UserRole

data class UserResponseDto(
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole,
    val createdAt: String,
    val updatedAt: String,
    val token: String?
) 