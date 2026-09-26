package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meal_log_items")
data class MealLogItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val mealLogId: String,
    val nutritionItemId: String? = null,
    val quantityG: Double,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)