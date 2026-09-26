package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.UserTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTargetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserTarget(target: UserTargetEntity)

    @Query("SELECT * FROM user_targets WHERE userId = :userId ORDER BY effectiveDate DESC LIMIT 1")
    fun getLatestUserTargetFlow(userId: String): Flow<UserTargetEntity?>

    @Query("SELECT * FROM user_targets WHERE userId = :userId ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getLatestUserTarget(userId: String): UserTargetEntity?
}