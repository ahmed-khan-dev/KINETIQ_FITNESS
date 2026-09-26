package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.ExerciseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(log: ExerciseLogEntity)

    @Query("SELECT * FROM exercise_logs WHERE userId = :userId AND completedAt >= :startTimestamp AND completedAt <= :endTimestamp")
    fun getLogsForDateRangeFlow(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_logs WHERE userId = :userId AND completedAt >= :startTimestamp AND completedAt <= :endTimestamp")
    suspend fun getLogsForDateRange(userId: String, startTimestamp: Long, endTimestamp: Long): List<ExerciseLogEntity>

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE userId = :userId AND completedAt >= :startTimestamp AND completedAt <= :endTimestamp")
    suspend fun getCompletedWorkoutCountForToday(userId: String, startTimestamp: Long, endTimestamp: Long): Int
}