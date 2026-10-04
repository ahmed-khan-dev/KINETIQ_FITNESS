package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(weightLog: WeightLogEntity)

    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY loggedAt DESC LIMIT 1")
    fun getLatestWeightFlow(userId: String): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY loggedAt DESC LIMIT 1")
    suspend fun getLatestWeight(userId: String): WeightLogEntity?

    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY loggedAt DESC")
    fun getAllWeightLogsFlow(userId: String): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE userId = :userId AND loggedAt >= :startTimestamp AND loggedAt <= :endTimestamp ORDER BY loggedAt DESC LIMIT 1")
    suspend fun getTodayWeightLog(userId: String, startTimestamp: Long, endTimestamp: Long): WeightLogEntity?

    @androidx.room.Update
    suspend fun updateWeightLog(weightLog: WeightLogEntity)
}