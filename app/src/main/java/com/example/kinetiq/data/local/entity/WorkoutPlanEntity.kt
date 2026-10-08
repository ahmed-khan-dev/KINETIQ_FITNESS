package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val weekStartDate: Long = System.currentTimeMillis(),
    val splitType: String,
    val planType: String = "SYSTEM",
    val cycleWeekNumber: Int = 1,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)