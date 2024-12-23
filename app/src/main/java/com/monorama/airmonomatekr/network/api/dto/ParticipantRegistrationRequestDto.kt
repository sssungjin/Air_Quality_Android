package com.monorama.airmonomatekr.network.api.dto

import com.monorama.airmonomatekr.data.model.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ParticipantRegistrationRequestDto(
    @Email @NotBlank val email: String,
    @NotBlank val password: String,
    @NotBlank val name: String,
    @NotNull val role: UserRole // UserRole은 enum으로 정의되어 있어야 합니다.
) 