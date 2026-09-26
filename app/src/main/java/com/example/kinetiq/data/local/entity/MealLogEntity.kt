package com.example.kinetiq.data.local.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "''")
    val mealName: String = "",
    @ColumnInfo(defaultValue = "0")
    val calories: Int = 0,
    @ColumnInfo(defaultValue = "0.0")
    val proteinG: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val carbsG: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val fatG: Double = 0.0,
    val recognitionConfidence: Double? = null,
    val gpsLogId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
