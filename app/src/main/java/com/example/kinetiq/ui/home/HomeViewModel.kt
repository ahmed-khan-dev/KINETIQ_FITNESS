package com.example.kinetiq.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetiq.KinetiqApplication
import com.example.kinetiq.data.local.entity.GpsLogEntity
import com.example.kinetiq.data.local.entity.MealLogEntity
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import com.example.kinetiq.data.local.entity.StreakEntity
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
    val gpsSummary: String = "No location recorded yet."
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
                val meals = repository.getTodayMealsFlow(userId, dayStart, nextDay - 1).first()
                val workout = loadWorkoutSummary()
                val savedStreak = repository.streakDao.getStreak(userId)
                val photo = repository.progressPhotoDao.getLatestProgressPhoto(userId)
                val gps = repository.gpsLogDao.getLatestGpsLogFlow().first()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    dateLabel = DateFormat.getDateInstance(DateFormat.FULL).format(Date(now)),
                    workoutTitle = workout.title,
                    workoutMeta = workout.meta,
                    workoutStatus = workout.status,
                    workoutExercises = workout.exercises,
                    hasWorkout = workout.hasWorkout,
                    meals = meals,
                    mealSummary = summarizeMeals(meals),
                    mealDetails = meals.joinToString("\n") { meal ->
                        "${meal.mealSlot}: ${meal.mealName} · ${meal.calories} kcal"
                    },
                    currentStreak = activeStreak(savedStreak, now),
                    longestStreak = savedStreak?.longestStreak ?: 0,
                    latestPhoto = photo,
                    gpsSummary = formatGps(gps)
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
        val session = repository.getSessionsForPlan(plan.id).firstOrNull() ?: return WorkoutSummary()
        val sessionExercises = repository.getSessionExercises(session.id)
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
            title = session.sessionName,
            meta = "${dayOfWeekLabel(session.dayOfWeek)} · ${exerciseLines.size} exercises · ${session.estimatedDurationMin} min",
            status = if (isComplete) "Complete · $completedSets/$totalSets sets" else "$completedSets/$totalSets sets completed",
            exercises = exerciseLines.joinToString("\n"),
            hasWorkout = true
        )
    }

    private fun loggedSetCount(log: com.example.kinetiq.data.local.entity.ExerciseLogEntity): Int =
        log.loggedSets.substringBefore('x').trim().toIntOrNull()?.coerceAtLeast(0) ?: 1

    private fun summarizeMeals(meals: List<MealLogEntity>): String {
        if (meals.isEmpty()) return "No meals logged today."
        val calories = meals.sumOf { it.calories }
        val protein = meals.sumOf { it.proteinG }
        val carbs = meals.sumOf { it.carbsG }
        val fat = meals.sumOf { it.fatG }
        return "${meals.size} ${if (meals.size == 1) "meal" else "meals"} · $calories kcal\nP ${protein.oneDecimal()}g · C ${carbs.oneDecimal()}g · F ${fat.oneDecimal()}g"
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
