package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exercise_library")
data class ExerciseLibraryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val equipmentTag: String,
    val difficulty: String,
    val metValue: Double = 5.0,
    val videoUrl: String? = null,
    val instructions: String,
    val exclusionTags: String? = null,
    val progressionType: String = "linear",
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultRestSec: Int = 60,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)