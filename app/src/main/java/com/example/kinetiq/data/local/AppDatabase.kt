package com.example.kinetiq.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kinetiq.data.local.dao.AppSessionDao
import com.example.kinetiq.data.local.dao.AppSettingsDao
import com.example.kinetiq.data.local.dao.DietaryPreferenceDao
import com.example.kinetiq.data.local.dao.ExerciseLibraryDao
import com.example.kinetiq.data.local.dao.ExerciseLogDao
import com.example.kinetiq.data.local.dao.GpsLogDao
import com.example.kinetiq.data.local.dao.MealLogDao
import com.example.kinetiq.data.local.dao.ProgressPhotoDao
import com.example.kinetiq.data.local.dao.StreakDao
import com.example.kinetiq.data.local.dao.UserDao
import com.example.kinetiq.data.local.dao.WeightLogDao
import com.example.kinetiq.data.local.dao.WorkoutPlanDao
import com.example.kinetiq.data.local.entity.AppSessionEntity
import com.example.kinetiq.data.local.entity.AppSettingsEntity
import com.example.kinetiq.data.local.entity.BadgeEntity
import com.example.kinetiq.data.local.entity.DailySummaryEntity
import com.example.kinetiq.data.local.entity.DietaryPreferenceEntity
import com.example.kinetiq.data.local.entity.ExerciseLibraryEntity
import com.example.kinetiq.data.local.entity.ExerciseLogEntity
import com.example.kinetiq.data.local.entity.FriendshipEntity
import com.example.kinetiq.data.local.entity.GpsLogEntity
import com.example.kinetiq.data.local.entity.MealLogEntity
import com.example.kinetiq.data.local.entity.MealLogItemEntity
import com.example.kinetiq.data.local.entity.MealPlanEntity
import com.example.kinetiq.data.local.entity.NotificationPreferenceEntity
import com.example.kinetiq.data.local.entity.NutritionItemEntity
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import com.example.kinetiq.data.local.entity.RecipeEntity
import com.example.kinetiq.data.local.entity.RecipeIngredientEntity
import com.example.kinetiq.data.local.entity.SessionExerciseEntity
import com.example.kinetiq.data.local.entity.StreakEntity
import com.example.kinetiq.data.local.entity.UserBadgeEntity
import com.example.kinetiq.data.local.entity.UserEntity
import com.example.kinetiq.data.local.entity.UserProfileEntity
import com.example.kinetiq.data.local.entity.UserTargetEntity
import com.example.kinetiq.data.local.entity.WeightLogEntity
import com.example.kinetiq.data.local.entity.WorkoutPlanEntity
import com.example.kinetiq.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.kinetiq.data.local.dao.UserTargetDao

@Database(
    entities = [
        UserEntity::class,
        UserProfileEntity::class,
        DietaryPreferenceEntity::class,
        UserTargetEntity::class,
        ExerciseLibraryEntity::class,
        WorkoutPlanEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        ExerciseLogEntity::class,
        NutritionItemEntity::class,
        MealLogEntity::class,
        MealLogItemEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        MealPlanEntity::class,
        WeightLogEntity::class,
        ProgressPhotoEntity::class,
        DailySummaryEntity::class,
        BadgeEntity::class,
        UserBadgeEntity::class,
        StreakEntity::class,
        NotificationPreferenceEntity::class,
        FriendshipEntity::class,
        GpsLogEntity::class,
        AppSessionEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun dietaryPreferenceDao(): DietaryPreferenceDao
    abstract fun userTargetDao(): UserTargetDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun gpsLogDao(): GpsLogDao
    abstract fun appSessionDao(): AppSessionDao
    abstract fun streakDao(): StreakDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kinetiq_database.db"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    val dao = database.exerciseLibraryDao()
                    if (dao.getExerciseCount() == 0) {
                        dao.insertExercises(ExerciseSeedData.initialExercises)
                    }
                }
            }
        }
    }
}