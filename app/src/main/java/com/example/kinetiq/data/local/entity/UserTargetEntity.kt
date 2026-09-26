package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_targets")
data class UserTargetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val bmr: Double = 1800.0,
    val tdee: Double = 2300.0,
    val calorieTarget: Int = 2200,
    val proteinG: Double = 150.0,
    val carbsG: Double = 220.0,
    val fatG: Double = 70.0,
    val effectiveDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)