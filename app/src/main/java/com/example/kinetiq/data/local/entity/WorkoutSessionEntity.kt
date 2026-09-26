package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val dayOfWeek: Int,
    val sessionName: String,
    val estimatedDurationMin: Int = 45,
    val estimatedCalories: Int = 300,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)