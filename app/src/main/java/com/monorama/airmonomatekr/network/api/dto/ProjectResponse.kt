package com.monorama.airmonomatekr.network.api.dto

data class ProjectResponse(
    val projectId: Long,
    val projectName: String,
    val description: String,
    val createdAt: String
)
