package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val summaryDate: Long = System.currentTimeMillis(),
    val workoutsCompleted: Int = 0,
    val workoutsPlanned: Int = 0,
    val caloriesIn: Int = 0,
    val calorieTarget: Int = 2000,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)