package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.MealLogEntity
import com.example.kinetiq.data.local.entity.MealLogItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(mealLog: MealLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLogItems(items: List<MealLogItemEntity>)

    @Query("SELECT * FROM meal_logs WHERE userId = :userId AND loggedAt >= :startTimestamp AND loggedAt <= :endTimestamp ORDER BY loggedAt DESC")
    fun getMealsForTodayFlow(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE userId = :userId ORDER BY loggedAt DESC")
    fun getAllMealsFlow(userId: String): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_log_items WHERE mealLogId = :mealLogId")
    suspend fun getMealLogItems(mealLogId: String): List<MealLogItemEntity>

    @Query("SELECT SUM(CASE WHEN m.calories > 0 THEN m.calories ELSE COALESCE((SELECT SUM(i.calories) FROM meal_log_items i WHERE i.mealLogId = m.id), 0) END) FROM meal_logs m WHERE m.userId = :userId AND m.loggedAt >= :startTimestamp AND m.loggedAt <= :endTimestamp")
    suspend fun getTodayTotalCalories(userId: String, startTimestamp: Long, endTimestamp: Long): Int?
}
