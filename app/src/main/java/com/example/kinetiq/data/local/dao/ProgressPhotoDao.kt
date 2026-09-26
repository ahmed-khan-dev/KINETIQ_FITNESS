package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressPhoto(photo: ProgressPhotoEntity)

    @Query("SELECT * FROM progress_photos WHERE userId = :userId ORDER BY takenAt DESC LIMIT 1")
    fun getLatestProgressPhotoFlow(userId: String): Flow<ProgressPhotoEntity?>

    @Query("SELECT * FROM progress_photos WHERE userId = :userId ORDER BY takenAt DESC LIMIT 1")
    suspend fun getLatestProgressPhoto(userId: String): ProgressPhotoEntity?

    @Query("SELECT * FROM progress_photos WHERE userId = :userId ORDER BY takenAt DESC")
    fun getAllProgressPhotosFlow(userId: String): Flow<List<ProgressPhotoEntity>>
}