package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.GpsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsLog(gpsLog: GpsLogEntity)

    @Query("SELECT * FROM gps_logs WHERE id = :id LIMIT 1")
    suspend fun getGpsLogById(id: String): GpsLogEntity?

    @Query("SELECT * FROM gps_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestGpsLogFlow(): Flow<GpsLogEntity?>
}