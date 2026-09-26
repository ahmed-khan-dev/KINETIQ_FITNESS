package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakEntity)

    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    fun getStreakFlow(userId: String): Flow<StreakEntity?>

    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    suspend fun getStreak(userId: String): StreakEntity?
}