package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exercise_logs")
data class ExerciseLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionExerciseId: String,
    val userId: String,
    val loggedSets: String,
    val rpe: Int? = null,
    val completedAt: Long = System.currentTimeMillis(),
    val gpsLogId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)