package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val photoUrl: String? = null,
    val loggedAt: Long = System.currentTimeMillis(),
    val mealSlot: String,
    val recognitionConfidence: Double? = null,
    val gpsLogId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)