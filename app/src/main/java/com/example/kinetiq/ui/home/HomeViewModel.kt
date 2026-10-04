package com.example.kinetiq.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetiq.KinetiqApplication
import com.example.kinetiq.data.local.entity.GpsLogEntity
import com.example.kinetiq.data.local.entity.MealLogEntity
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import com.example.kinetiq.data.local.entity.StreakEntity
import com.example.kinetiq.data.local.entity.UserProfileEntity
import com.example.kinetiq.data.local.entity.UserTargetEntity
import com.example.kinetiq.data.local.entity.WorkoutPlanEntity
import com.example.kinetiq.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val dateLabel: String = "",
    val workoutTitle: String = "No workout plan yet",
    val workoutMeta: String = "Open Workout to create a plan.",
    val workoutStatus: String = "",
    val workoutExercises: String = "",
    val hasWorkout: Boolean = false,
    val meals: List<MealLogEntity> = emptyList(),
    val mealSummary: String = "No meals logged today.",
    val mealDetails: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val latestPhoto: ProgressPhotoEntity? = null,
    val gpsSummary: String = "No location recorded yet.",
    val consumedCalories: Int = 0,
    val targetCalories: Int = 2000,
    val proteinG: Double = 0.0,
    val targetProteinG: Double = 140.0,
    val carbsG: Double = 0.0,
    val targetCarbsG: Double = 270.0,
    val fatG: Double = 0.0,
    val targetFatG: Double = 60.0,
    val calorieProgressText: String = "",
    val macroInsightText: String = "",
    val weightTrendInsight: String = "",
    val coachingTip: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as KinetiqApplication).repository
    private val userId = "default_user_id"
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val now = System.currentTimeMillis()
                val dayStart = startOfLocalDay(now)
                val nextDay = Calendar.getInstance().apply {
                    timeInMillis = dayStart
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis

                val profile = repository.getProfile(userId)
                val target = repository.getUserTarget(userId)
                val meals = repository.getTodayMealsFlow(userId, dayStart, nextDay - 1).first()
                val workout = loadWorkoutSummary()
                val savedStreak = repository.streakDao.getStreak(userId)
                val photo = repository.progressPhotoDao.getLatestProgressPhoto(userId)
                val gps = repository.gpsLogDao.getLatestGpsLogFlow().first()
                val latestWeight = repository.weightLogDao.getLatestWeight(userId)

                val calorieSummary = generateCalorieInsight(meals, target)
                val weightInsight = generateWeightInsight(profile, latestWeight?.weightKg)
                val coachingTipText = generateCoachingTip(profile, target, meals, workout.hasWorkout)

                val consumedCal = meals.sumOf { it.calories }
                val targetCal = target?.calorieTarget ?: 2000
                val protein = meals.sumOf { it.proteinG }
                val targetProtein = target?.proteinG ?: 140.0
                val carbs = meals.sumOf { it.carbsG }
                val targetCarbs = target?.carbsG ?: 270.0
                val fat = meals.sumOf { it.fatG }
                val targetFat = target?.fatG ?: 60.0

                _uiState.value = HomeUiState(
                    isLoading = false,
                    dateLabel = DateFormat.getDateInstance(DateFormat.FULL).format(Date(now)),
                    workoutTitle = workout.title,
                    workoutMeta = workout.meta,
                    workoutStatus = workout.status,
                    workoutExercises = workout.exercises,
                    hasWorkout = workout.hasWorkout,
                    meals = meals,
                    mealSummary = calorieSummary.first,
                    mealDetails = calorieSummary.second,
                    currentStreak = activeStreak(savedStreak, now),
                    longestStreak = savedStreak?.longestStreak ?: 0,
                    latestPhoto = photo,
                    gpsSummary = formatGps(gps),
                    consumedCalories = consumedCal,
                    targetCalories = targetCal,
                    proteinG = protein,
                    targetProteinG = targetProtein,
                    carbsG = carbs,
                    targetCarbsG = targetCarbs,
                    fatG = fat,
                    targetFatG = targetFat,
                    weightTrendInsight = weightInsight,
                    coachingTip = coachingTipText
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Could not load today's summary."
                )
            }
        }
    }

    private suspend fun loadWorkoutSummary(): WorkoutSummary {
        val plan = repository.getActiveWorkoutPlan(userId) ?: return WorkoutSummary()
        val sessions = repository.getSessionsForPlan(plan.id)
        if (sessions.isEmpty()) return WorkoutSummary()

        val currentDayOfWeek = currentDayOfWeekIndex()
        val todaySession = sessions.firstOrNull { it.dayOfWeek == currentDayOfWeek }
            ?: sessions.firstOrNull() ?: return WorkoutSummary()

        val sessionExercises = repository.getSessionExercises(todaySession.id)
        if (sessionExercises.isEmpty()) return WorkoutSummary()

        val exerciseNames = repository.getAllExercises().associateBy({ it.id }, { it.name })
        var totalSets = 0
        var completedSets = 0
        val exerciseLines = sessionExercises.mapNotNull { row ->
            val exerciseName = exerciseNames[row.exerciseId] ?: return@mapNotNull null
            val completed = repository.getExerciseLogsForSessionExercise(row.id)
                .sumOf(::loggedSetCount).coerceAtMost(row.targetSets)
            totalSets += row.targetSets
            completedSets += completed
            "$exerciseName · $completed/${row.targetSets} sets · ${row.targetReps} reps"
        }
        if (exerciseLines.isEmpty()) return WorkoutSummary()
        val isComplete = totalSets > 0 && completedSets >= totalSets

        return WorkoutSummary(
            title = todaySession.sessionName,
            meta = "${dayOfWeekLabel(todaySession.dayOfWeek)} · ${exerciseLines.size} exercises · ${todaySession.estimatedDurationMin} min",
            status = if (isComplete) "Complete · $completedSets/$totalSets sets" else "$completedSets/$totalSets sets completed",
            exercises = exerciseLines.joinToString("\n"),
            hasWorkout = true
        )
    }

    private fun loggedSetCount(log: com.example.kinetiq.data.local.entity.ExerciseLogEntity): Int =
        log.loggedSets.substringBefore('x').trim().toIntOrNull()?.coerceAtLeast(0) ?: 1

    private fun generateCalorieInsight(meals: List<MealLogEntity>, target: UserTargetEntity?): Pair<String, String> {
        if (meals.isEmpty()) {
            val targetCal = target?.calorieTarget ?: 2000
            return "No meals logged today." to "Goal: $targetCal kcal · 0 kcal logged so far."
        }
        val totalCalories = meals.sumOf { it.calories }
        val totalProtein = meals.sumOf { it.proteinG }
        val totalCarbs = meals.sumOf { it.carbsG }
        val totalFat = meals.sumOf { it.fatG }

        val targetCal = target?.calorieTarget ?: 2000
        val remainingCal = targetCal - totalCalories
        val statusText = if (remainingCal >= 0) {
            "$totalCalories / $targetCal kcal ($remainingCal kcal remaining)"
        } else {
            "$totalCalories / $targetCal kcal (${Math.abs(remainingCal)} kcal over target)"
        }

        val details = "${meals.size} ${if (meals.size == 1) "meal" else "meals"} logged\n" +
                "Protein: ${totalProtein.oneDecimal()}g / ${target?.proteinG?.oneDecimal() ?: "140"}g · " +
                "Carbs: ${totalCarbs.oneDecimal()}g · " +
                "Fat: ${totalFat.oneDecimal()}g"

        return statusText to details
    }

    private fun generateWeightInsight(profile: UserProfileEntity?, latestWeight: Double?): String {
        if (profile == null) return "Complete profile to track weight trends."
        if (latestWeight == null) return "Current weight: ${profile.weightKg} kg · Log weight in Progress tab."

        val delta = latestWeight - profile.weightKg
        val deltaFormatted = String.format("%.1f", Math.abs(delta))
        return when {
            delta < -0.2 -> "Current weight: $latestWeight kg ($deltaFormatted kg down from start) · Progressing well toward ${profile.goal}!"
            delta > 0.2 -> "Current weight: $latestWeight kg (+$deltaFormatted kg up from start) · Aligned with ${profile.goal}."
            else -> "Current weight: $latestWeight kg · Maintaining consistent weight."
        }
    }

    private fun generateCoachingTip(
        profile: UserProfileEntity?,
        target: UserTargetEntity?,
        meals: List<MealLogEntity>,
        hasWorkout: Boolean
    ): String {
        if (profile == null) return "Complete your profile to receive AI fitness coaching tips!"
        val consumedCalories = meals.sumOf { it.calories }
        val targetCalories = target?.calorieTarget ?: 2000

        return when (profile.goal.lowercase()) {
            "weight loss" -> {
                if (consumedCalories < targetCalories) {
                    "Maintain a moderate deficit. You have ${targetCalories - consumedCalories} kcal remaining for today."
                } else {
                    "Daily calorie limit reached. Focus on hydration and rest tonight!"
                }
            }
            "muscle gain" -> {
                "Target $targetCalories kcal and hit your protein goal to maximize muscle hypertrophy."
            }
            else -> "Stay consistent with your ${profile.daysPerWeek}-day workout schedule and balanced nutrition!"
        }
    }

    private fun formatGps(gps: GpsLogEntity?): String {
        if (gps == null) return "No location recorded yet."
        val recordedAt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(gps.timestamp))
        val coordinates = if (gps.gpsOk && gps.latitude != null && gps.longitude != null) {
            "${gps.latitude.fourDecimals()}, ${gps.longitude.fourDecimals()}"
        } else "Location unavailable"
        return "$coordinates · $recordedAt"
    }

    private fun activeStreak(streak: StreakEntity?, now: Long): Int {
        if (streak == null) return 0
        val lastDay = startOfLocalDay(streak.lastActiveDate)
        val today = startOfLocalDay(now)
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        return if (lastDay == today || lastDay == yesterday) streak.currentStreak else 0
    }

    private fun startOfLocalDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun currentDayOfWeekIndex(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    private fun Double.oneDecimal() = "%.1f".format(this)
    private fun Double.fourDecimals() = "%.4f".format(this)

    private fun dayOfWeekLabel(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> "Day $dayOfWeek"
    }

    private data class WorkoutSummary(
        val title: String = "No workout plan yet",
        val meta: String = "Open Workout to create a plan.",
        val status: String = "",
        val exercises: String = "",
        val hasWorkout: Boolean = false
    )
}