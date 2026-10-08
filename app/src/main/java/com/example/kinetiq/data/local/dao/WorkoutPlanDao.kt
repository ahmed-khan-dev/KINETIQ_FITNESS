package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.SessionExerciseEntity
import com.example.kinetiq.data.local.entity.WorkoutPlanEntity
import com.example.kinetiq.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlanEntity)

    @Query("SELECT * FROM workout_plans WHERE userId = :userId AND status = 'active' ORDER BY createdAt DESC LIMIT 1")
    fun getActiveWorkoutPlanFlow(userId: String): Flow<WorkoutPlanEntity?>

    @Query("SELECT * FROM workout_plans WHERE userId = :userId AND status = 'active' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveWorkoutPlan(userId: String): WorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSessions(sessions: List<WorkoutSessionEntity>)

    @Query("SELECT * FROM workout_sessions WHERE planId = :planId ORDER BY dayOfWeek ASC")
    fun getSessionsForPlanFlow(planId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE planId = :planId ORDER BY dayOfWeek ASC")
    suspend fun getSessionsForPlan(planId: String): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionExercises(sessionExercises: List<SessionExerciseEntity>)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getSessionExercisesFlow(sessionId: String): Flow<List<SessionExerciseEntity>>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getSessionExercises(sessionId: String): List<SessionExerciseEntity>

    @Query("DELETE FROM session_exercises WHERE id = :sessionExerciseId")
    suspend fun deleteSessionExercise(sessionExerciseId: String)

    @Query("UPDATE session_exercises SET targetSets = :sets, targetReps = :reps, targetRestSec = :restSec, updatedAt = :updatedAt WHERE id = :sessionExerciseId")
    suspend fun updateSessionExercise(sessionExerciseId: String, sets: Int, reps: Int, restSec: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE workout_sessions SET sessionName = :newName, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionName(sessionId: String, newName: String, updatedAt: Long = System.currentTimeMillis())
}
