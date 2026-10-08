package com.example.kinetiq.data.repository

import com.example.kinetiq.data.local.AppDatabase
import com.example.kinetiq.data.local.entity.AppSessionEntity
import com.example.kinetiq.data.local.entity.DietaryPreferenceEntity
import com.example.kinetiq.data.local.entity.ExerciseLibraryEntity
import com.example.kinetiq.data.local.entity.ExerciseLogEntity
import com.example.kinetiq.data.local.entity.GpsLogEntity
import com.example.kinetiq.data.local.entity.MealLogEntity
import com.example.kinetiq.data.local.entity.MealLogItemEntity
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import com.example.kinetiq.data.local.entity.StreakEntity
import com.example.kinetiq.data.local.entity.UserProfileEntity
import com.example.kinetiq.data.local.entity.WeightLogEntity
import com.example.kinetiq.data.local.entity.WorkoutPlanEntity
import com.example.kinetiq.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

import androidx.room.withTransaction
import com.example.kinetiq.data.local.entity.SessionExerciseEntity
import com.example.kinetiq.data.local.entity.UserTargetEntity

class AppRepository(private val database: AppDatabase) {

    val userDao = database.userDao()
    val dietaryPreferenceDao = database.dietaryPreferenceDao()
    val userTargetDao = database.userTargetDao()
    val exerciseLibraryDao = database.exerciseLibraryDao()
    val workoutPlanDao = database.workoutPlanDao()
    val exerciseLogDao = database.exerciseLogDao()
    val mealLogDao = database.mealLogDao()
    val weightLogDao = database.weightLogDao()
    val progressPhotoDao = database.progressPhotoDao()
    val gpsLogDao = database.gpsLogDao()
    val appSessionDao = database.appSessionDao()
    val streakDao = database.streakDao()
    val appSettingsDao = database.appSettingsDao()

    // Profile & Dietary Preferences & Targets
    suspend fun saveProfile(profile: UserProfileEntity) = userDao.insertOrUpdateProfile(profile)
    fun getProfileFlow(userId: String = "default_user_id"): Flow<UserProfileEntity?> = userDao.getProfileFlow(userId)
    suspend fun getProfile(userId: String = "default_user_id"): UserProfileEntity? = userDao.getProfile(userId)

    suspend fun saveDietaryPreference(pref: DietaryPreferenceEntity) = dietaryPreferenceDao.insertOrUpdateDietaryPreference(pref)
    suspend fun getDietaryPreference(userId: String = "default_user_id"): DietaryPreferenceEntity? = dietaryPreferenceDao.getDietaryPreference(userId)
    fun getDietaryPreferenceFlow(userId: String = "default_user_id"): Flow<DietaryPreferenceEntity?> = dietaryPreferenceDao.getDietaryPreferenceFlow(userId)

    fun getUserTargetFlow(userId: String = "default_user_id"): Flow<UserTargetEntity?> = userTargetDao.getLatestUserTargetFlow(userId)
    suspend fun getUserTarget(userId: String = "default_user_id"): UserTargetEntity? = userTargetDao.getLatestUserTarget(userId)

    suspend fun saveFullProfileAndTargets(
        profile: UserProfileEntity,
        preference: DietaryPreferenceEntity,
        target: UserTargetEntity
    ) {
        database.withTransaction {
            userDao.insertOrUpdateProfile(profile)
            dietaryPreferenceDao.insertOrUpdateDietaryPreference(preference)
            userTargetDao.insertOrUpdateUserTarget(target)
        }
    }

    // Exercises
    fun getAllExercisesFlow(): Flow<List<ExerciseLibraryEntity>> = exerciseLibraryDao.getAllExercisesFlow()
    suspend fun getAllExercises(): List<ExerciseLibraryEntity> = exerciseLibraryDao.getAllExercises()

    suspend fun saveCustomExercise(
        name: String,
        muscleGroup: String = "General",
        equipment: String = "Bodyweight",
        instructions: String = "Custom exercise added by user."
    ): ExerciseLibraryEntity {
        val exercise = ExerciseLibraryEntity(
            id = java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            muscleGroup = muscleGroup,
            equipmentTag = equipment,
            difficulty = "Intermediate",
            instructions = instructions
        )
        exerciseLibraryDao.insertExercise(exercise)
        return exercise
    }

    // Workout Plans & Logs
    suspend fun saveWorkoutPlan(plan: WorkoutPlanEntity) = workoutPlanDao.insertWorkoutPlan(plan)
    fun getActiveWorkoutPlanFlow(userId: String = "default_user_id"): Flow<WorkoutPlanEntity?> = workoutPlanDao.getActiveWorkoutPlanFlow(userId)
    suspend fun saveWorkoutSessions(sessions: List<WorkoutSessionEntity>) = workoutPlanDao.insertWorkoutSessions(sessions)

    suspend fun saveExerciseLog(log: ExerciseLogEntity) = exerciseLogDao.insertExerciseLog(log)

    suspend fun getActiveWorkoutPlan(userId: String = "default_user_id"): WorkoutPlanEntity? =
        workoutPlanDao.getActiveWorkoutPlan(userId)


    suspend fun getSessionsForPlan(planId: String): List<WorkoutSessionEntity> =
        workoutPlanDao.getSessionsForPlan(planId)

    suspend fun getWorkoutSession(sessionId: String): WorkoutSessionEntity? =
        workoutPlanDao.getSessionById(sessionId)

    suspend fun getSessionExercises(sessionId: String): List<SessionExerciseEntity> =
        workoutPlanDao.getSessionExercises(sessionId)

    suspend fun getExerciseLogsForSessionExercise(sessionExerciseId: String): List<ExerciseLogEntity> =
        exerciseLogDao.getLogsForSessionExercise(sessionExerciseId)

    suspend fun saveWorkoutPlanWithSessions(
        plan: WorkoutPlanEntity,
        sessions: List<WorkoutSessionEntity>,
        exercises: List<SessionExerciseEntity>
    ) {
        database.withTransaction {
            workoutPlanDao.insertWorkoutPlan(plan)
            if (sessions.isNotEmpty()) {
                workoutPlanDao.insertWorkoutSessions(sessions)
            }
            if (exercises.isNotEmpty()) {
                workoutPlanDao.insertSessionExercises(exercises)
            }
        }
    }

    suspend fun repeatWorkoutPlanNewWeek(currentPlan: WorkoutPlanEntity): WorkoutPlanEntity {
        return database.withTransaction {
            val archivedPlan = currentPlan.copy(status = "completed", updatedAt = System.currentTimeMillis())
            workoutPlanDao.insertWorkoutPlan(archivedPlan)

            val newPlanId = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val newPlan = WorkoutPlanEntity(
                id = newPlanId,
                userId = currentPlan.userId,
                weekStartDate = now,
                splitType = currentPlan.splitType,
                planType = currentPlan.planType,
                cycleWeekNumber = currentPlan.cycleWeekNumber + 1,
                status = "active",
                createdAt = now,
                updatedAt = now
            )
            workoutPlanDao.insertWorkoutPlan(newPlan)

            val currentSessions = workoutPlanDao.getSessionsForPlan(currentPlan.id)
            val newSessions = mutableListOf<WorkoutSessionEntity>()
            val newExercises = mutableListOf<SessionExerciseEntity>()

            currentSessions.forEach { oldSession ->
                val newSessionId = java.util.UUID.randomUUID().toString()
                val newSession = oldSession.copy(
                    id = newSessionId,
                    planId = newPlanId,
                    createdAt = now,
                    updatedAt = now
                )
                newSessions.add(newSession)

                val oldExercises = workoutPlanDao.getSessionExercises(oldSession.id)
                oldExercises.forEach { oldEx ->
                    val newEx = oldEx.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        sessionId = newSessionId,
                        createdAt = now,
                        updatedAt = now
                    )
                    newExercises.add(newEx)
                }
            }

            if (newSessions.isNotEmpty()) workoutPlanDao.insertWorkoutSessions(newSessions)
            if (newExercises.isNotEmpty()) workoutPlanDao.insertSessionExercises(newExercises)

            newPlan
        }
    }

    // Meals
    suspend fun saveMealLog(mealLog: MealLogEntity, items: List<MealLogItemEntity> = emptyList()): Long {
        return database.withTransaction {
            val id = mealLogDao.insertMealLog(mealLog)
            if (items.isNotEmpty()) {
                mealLogDao.insertMealLogItems(items)
            }
            id
        }
    }
    fun getTodayMealsFlow(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<List<MealLogEntity>> =
        mealLogDao.getMealsForTodayFlow(userId, startTimestamp, endTimestamp)

    fun getAllMealsFlow(userId: String): Flow<List<MealLogEntity>> = mealLogDao.getAllMealsFlow(userId)

    suspend fun deleteMealLog(mealLog: MealLogEntity) = mealLogDao.deleteMealLog(mealLog)

    suspend fun updateMealLog(mealLog: MealLogEntity) = mealLogDao.updateMealLog(mealLog)

    // Weight
    suspend fun saveWeightLog(weightLog: WeightLogEntity) = weightLogDao.insertWeightLog(weightLog)
    suspend fun updateWeightLog(weightLog: WeightLogEntity) = weightLogDao.updateWeightLog(weightLog)
    suspend fun getTodayWeightLog(userId: String, startTimestamp: Long, endTimestamp: Long) = weightLogDao.getTodayWeightLog(userId, startTimestamp, endTimestamp)
    fun getLatestWeightFlow(userId: String): Flow<WeightLogEntity?> = weightLogDao.getLatestWeightFlow(userId)
    fun getAllWeightLogsFlow(userId: String): Flow<List<WeightLogEntity>> = weightLogDao.getAllWeightLogsFlow(userId)

    // Progress Photos
    suspend fun saveProgressPhoto(photo: ProgressPhotoEntity) = progressPhotoDao.insertProgressPhoto(photo)
    fun getLatestProgressPhotoFlow(userId: String): Flow<ProgressPhotoEntity?> = progressPhotoDao.getLatestProgressPhotoFlow(userId)
    fun getAllProgressPhotosFlow(userId: String): Flow<List<ProgressPhotoEntity>> = progressPhotoDao.getAllProgressPhotosFlow(userId)

    // GPS Logs
    suspend fun saveGpsLog(gpsLog: GpsLogEntity) = gpsLogDao.insertGpsLog(gpsLog)

    // Session & Auth
    suspend fun getAppSession(userId: String = "default_user_id"): AppSessionEntity? {
        return appSessionDao.getSession(userId)
    }

    fun getAppSessionFlow(userId: String = "default_user_id"): Flow<AppSessionEntity?> {
        return appSessionDao.getSessionFlow(userId)
    }

    suspend fun saveAppSession(session: AppSessionEntity) = appSessionDao.insertOrUpdateSession(session)

    suspend fun updateSessionSuccess(userId: String = "default_user_id") {
        val now = System.currentTimeMillis()
        val currentSession = appSessionDao.getSession(userId)
        val updated = currentSession?.copy(
            isAuthenticated = true,
            lastActiveTimestamp = now,
            sessionExpiryTimestamp = now + com.example.kinetiq.utils.SecurityUtils.SESSION_TIMEOUT_MS
        ) ?: AppSessionEntity(
            userId = userId,
            isAuthenticated = true,
            lastActiveTimestamp = now,
            sessionExpiryTimestamp = now + com.example.kinetiq.utils.SecurityUtils.SESSION_TIMEOUT_MS
        )
        appSessionDao.insertOrUpdateSession(updated)
    }

    suspend fun setPinHash(pin: String, userId: String = "default_user_id") {
        val hash = com.example.kinetiq.utils.SecurityUtils.hashPin(pin)
        val currentSession = appSessionDao.getSession(userId)
        val updated = currentSession?.copy(pinHash = hash) ?: AppSessionEntity(
            userId = userId,
            pinHash = hash
        )
        appSessionDao.insertOrUpdateSession(updated)
    }

    suspend fun invalidateSession(userId: String = "default_user_id") {
        val currentSession = appSessionDao.getSession(userId)
        if (currentSession != null) {
            val updated = currentSession.copy(isAuthenticated = false)
            appSessionDao.insertOrUpdateSession(updated)
        }
    }

    // Streak
    suspend fun saveStreak(streak: StreakEntity) = streakDao.insertOrUpdateStreak(streak)
    fun getStreakFlow(userId: String): Flow<StreakEntity?> = streakDao.getStreakFlow(userId)
}
