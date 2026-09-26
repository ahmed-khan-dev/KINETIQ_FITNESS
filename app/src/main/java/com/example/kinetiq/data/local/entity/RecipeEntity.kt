package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dietType: String,
    val mealSlot: String,
    val prepTimeMin: Int,
    val servings: Int,
    val caloriesPerServing: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val instructions: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)