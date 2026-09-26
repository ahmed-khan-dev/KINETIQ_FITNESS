package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.AppSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: AppSessionEntity)

    @Query("SELECT * FROM app_sessions WHERE userId = :userId LIMIT 1")
    fun getSessionFlow(userId: String): Flow<AppSessionEntity?>

    @Query("SELECT * FROM app_sessions WHERE userId = :userId LIMIT 1")
    suspend fun getSession(userId: String): AppSessionEntity?
}