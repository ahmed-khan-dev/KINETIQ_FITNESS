package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.ExerciseLibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseLibraryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseLibraryEntity)

    @Query("SELECT COUNT(*) FROM exercise_library")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM exercise_library")
    fun getAllExercisesFlow(): Flow<List<ExerciseLibraryEntity>>

    @Query("SELECT * FROM exercise_library")
    suspend fun getAllExercises(): List<ExerciseLibraryEntity>

    @Query("SELECT * FROM exercise_library WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: String): ExerciseLibraryEntity?
}