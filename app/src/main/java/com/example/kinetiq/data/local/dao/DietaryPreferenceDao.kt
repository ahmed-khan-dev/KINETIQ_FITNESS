package com.example.kinetiq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kinetiq.data.local.entity.DietaryPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DietaryPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDietaryPreference(preference: DietaryPreferenceEntity)

    @Query("SELECT * FROM dietary_preferences WHERE userId = :userId LIMIT 1")
    fun getDietaryPreferenceFlow(userId: String): Flow<DietaryPreferenceEntity?>

    @Query("SELECT * FROM dietary_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getDietaryPreference(userId: String): DietaryPreferenceEntity?
}