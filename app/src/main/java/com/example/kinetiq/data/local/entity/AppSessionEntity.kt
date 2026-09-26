package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "app_sessions")
data class AppSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "default_user_id",
    val isAuthenticated: Boolean = false,
    val pinHash: String? = null,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val sessionExpiryTimestamp: Long = System.currentTimeMillis()
)