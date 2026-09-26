package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notification_preferences")
data class NotificationPreferenceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val workoutReminders: Boolean = true,
    val mealReminders: Boolean = true,
    val hydrationReminders: Boolean = false,
    val checkinReminders: Boolean = true,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)