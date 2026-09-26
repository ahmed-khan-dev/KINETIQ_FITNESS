package com.example.kinetiq.domain.model

data class UserSession(
    val isAuthenticated: Boolean = false,
    val lastAuthTimestamp: Long = 0L
)