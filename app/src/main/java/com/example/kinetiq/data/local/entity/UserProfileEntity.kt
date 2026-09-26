package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val age: Int = 25,
    val gender: String = "Male",
    val heightCm: Double = 175.0,
    val weightKg: Double = 70.0,
    val fitnessLevel: String = "Intermediate",
    val activityLevel: String = "Moderate",
    val goal: String = "Muscle Gain",
    val equipmentAccess: String = "Full Gym",
    val sessionMinutes: Int = 45,
    val daysPerWeek: Int = 4,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)