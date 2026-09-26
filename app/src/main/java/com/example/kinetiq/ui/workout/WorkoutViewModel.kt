package com.example.kinetiq.ui.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetiq.KinetiqApplication
import com.example.kinetiq.data.local.entity.DietaryPreferenceEntity
import com.example.kinetiq.data.local.entity.ExerciseLibraryEntity
import com.example.kinetiq.data.local.entity.ExerciseLogEntity
import com.example.kinetiq.data.local.entity.GpsLogEntity
import com.example.kinetiq.data.local.entity.SessionExerciseEntity
import com.example.kinetiq.data.local.entity.UserProfileEntity
import com.example.kinetiq.data.local.entity.WorkoutPlanEntity
import com.example.kinetiq.data.local.entity.WorkoutSessionEntity
import com.example.kinetiq.utils.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class WorkoutDayUi(
    val sessionId: String,
    val dayName: String,
    val title: String,
    val focus: String,
    val estimatedMinutes: Int,
    val exerciseCount: Int,
    val totalSets: Int,
    val exercises: List<WorkoutExerciseUi>
)

data class WorkoutExerciseUi(
    val sessionExerciseId: String,
    val exerciseId: String,
    val name: String,
    val targetMuscles: String,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val notes: String
)

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val hasProfile: Boolean = false,
    val profileMessage: String? = null,
    val activePlanId: String? = null,
    val planTitle: String = "",
    val planSummary: String = "",
    val weeklyPlan: List<WorkoutDayUi> = emptyList(),
    val todayWorkout: WorkoutDayUi? = null,
    val totalWorkoutMinutes: Int = 0,
    val totalExercises: Int = 0,
    val errorMessage: String? = null
)

data class WorkoutSessionUiState(
    val hasWorkoutData: Boolean = false,
    val isComplete: Boolean = false,
    val sessionName: String = "",
    val sessionIndex: Int = 0,
    val totalSessions: Int = 0,
    val exerciseName: String = "",
    val currentExerciseIndex: Int = 0,
    val totalExercises: Int = 0,
    val targetMuscles: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val totalSets: Int = 0,
    val completedSets: Int = 0,
    val restSeconds: Int = 0,
    val notes: String = "",
    val sessionExerciseId: String = "",
    val completionSummary: String = "",
    val currentSessionId: String = ""
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KinetiqApplication).repository
    private val userId = "default_user_id"

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow(WorkoutSessionUiState())
    val sessionState: StateFlow<WorkoutSessionUiState> = _sessionState.asStateFlow()

    private var cachedSessions: List<WorkoutDayUi> = emptyList()
    private var selectedSessionIndex: Int = 0

    fun loadWorkout(forceGenerate: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val profile = repository.getProfile(userId)
            val dietaryPreference = repository.getDietaryPreference(userId)
            if (profile == null) {
                cachedSessions = emptyList()
                selectedSessionIndex = 0
                _sessionState.value = WorkoutSessionUiState()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasProfile = false,
                    profileMessage = "Complete your profile to generate your personalized workout plan.",
                    weeklyPlan = emptyList(),
                    todayWorkout = null,
                    totalWorkoutMinutes = 0,
                    totalExercises = 0
                )
                return@launch
            }

            val activePlan = repository.getActiveWorkoutPlan(userId)
             if (!forceGenerate && activePlan != null) {
                 val mapped = buildDayUiFromPlanAsync(activePlan)
                 cachedSessions = mapped
                 selectedSessionIndex = mapped.indices.firstOrNull() ?: 0
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasProfile = true,
                    profileMessage = null,
                    activePlanId = activePlan.id,
                    planTitle = activePlan.splitType,
                    planSummary = "${mapped.size} workout sessions planned for this week",
                    weeklyPlan = mapped,
                    todayWorkout = mapped.firstOrNull(),
                    totalWorkoutMinutes = mapped.sumOf { it.estimatedMinutes },
                    totalExercises = mapped.sumOf { it.exerciseCount },
                    errorMessage = null
                )
                prepareSession(selectedSessionIndex)
                return@launch
            }

            val generatedPlan = generateWorkoutPlan(profile, dietaryPreference)
            cachedSessions = generatedPlan.second
            selectedSessionIndex = cachedSessions.indices.firstOrNull() ?: 0
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasProfile = true,
                profileMessage = null,
                activePlanId = generatedPlan.first.id,
                planTitle = generatedPlan.first.splitType,
                planSummary = "${generatedPlan.second.size} workout sessions planned for this week",
                weeklyPlan = generatedPlan.second,
                todayWorkout = generatedPlan.second.firstOrNull(),
                totalWorkoutMinutes = generatedPlan.second.sumOf { it.estimatedMinutes },
                totalExercises = generatedPlan.second.sumOf { it.exerciseCount },
                errorMessage = null
            )
            prepareSession(selectedSessionIndex)
        }
    }

    fun generateWorkoutPlan(force: Boolean = false) {
        loadWorkout(forceGenerate = force)
    }

    fun openSession(sessionIndex: Int) {
        if (cachedSessions.isEmpty()) return
        selectedSessionIndex = sessionIndex.coerceIn(0, cachedSessions.lastIndex)
        prepareSession(selectedSessionIndex)
    }

    fun prepareSession(sessionIndex: Int) {
        val session = cachedSessions.getOrNull(sessionIndex) ?: run {
            _sessionState.value = WorkoutSessionUiState()
            return
        }

        val firstExercise = session.exercises.firstOrNull()
        if (firstExercise == null) {
            _sessionState.value = WorkoutSessionUiState(
                hasWorkoutData = false,
                sessionName = session.title,
                sessionIndex = sessionIndex,
                totalSessions = cachedSessions.size,
                totalExercises = 0,
                totalSets = 0,
                currentSessionId = session.sessionId
            )
            return
        }

        val sessionTotalSets = session.exercises.sumOf { it.sets }
        _sessionState.value = WorkoutSessionUiState(
            hasWorkoutData = true,
            sessionName = session.title,
            sessionIndex = sessionIndex,
            totalSessions = cachedSessions.size,
            exerciseName = firstExercise.name,
            currentExerciseIndex = 0,
            totalExercises = session.exercises.size,
            targetMuscles = firstExercise.targetMuscles,
            sets = firstExercise.sets,
            reps = firstExercise.reps,
            totalSets = sessionTotalSets,
            completedSets = 0,
            restSeconds = firstExercise.restSeconds,
            notes = firstExercise.notes,
            sessionExerciseId = firstExercise.sessionExerciseId,
            currentSessionId = session.sessionId
        )
    }

    fun completeCurrentExercise(weight: String = "", repsOverride: Int? = null, setsOverride: Int? = null) {
         val currentState = _sessionState.value
         if (!currentState.hasWorkoutData) return

         val session = cachedSessions.getOrNull(currentState.sessionIndex) ?: return
         val exercise = session.exercises.getOrNull(currentState.currentExerciseIndex) ?: return

         val exerciseCompletedSets = setsOverride ?: exercise.sets
         val completedTotal = currentState.completedSets + exerciseCompletedSets.coerceAtLeast(0)
         val logEntry = if (repsOverride != null || weight.isNotBlank()) {
             val repsText = repsOverride ?: exercise.reps
             val weightText = if (weight.isNotBlank()) " @ ${weight.trim()} kg" else ""
             "$exerciseCompletedSets x $repsText$weightText"
         } else {
             "${exerciseCompletedSets} x ${exercise.reps}"
         }

         viewModelScope.launch {
             // Capture GPS location
             val gpsLog = LocationUtils.getCurrentLocation(getApplication())
             val savedGpsId = if (gpsLog.gpsOk) {
                 repository.saveGpsLog(gpsLog)
                 gpsLog.id
             } else {
                 repository.saveGpsLog(gpsLog)
                 gpsLog.id
             }

             val log = ExerciseLogEntity(
                 id = UUID.randomUUID().toString(),
                 sessionExerciseId = exercise.sessionExerciseId,
                 userId = userId,
                 loggedSets = logEntry,
                 rpe = 7,
                 completedAt = System.currentTimeMillis(),
                 gpsLogId = savedGpsId,
                 createdAt = System.currentTimeMillis(),
                 updatedAt = System.currentTimeMillis()
             )
             repository.saveExerciseLog(log)
         }

         val nextIndex = currentState.currentExerciseIndex + 1
         if (nextIndex < session.exercises.size) {
             val nextExercise = session.exercises[nextIndex]
             _sessionState.value = currentState.copy(
                 exerciseName = nextExercise.name,
                 currentExerciseIndex = nextIndex,
                 totalExercises = session.exercises.size,
                 targetMuscles = nextExercise.targetMuscles,
                 sets = nextExercise.sets,
                 reps = nextExercise.reps,
                 restSeconds = nextExercise.restSeconds,
                 notes = nextExercise.notes,
                 sessionExerciseId = nextExercise.sessionExerciseId,
                 completedSets = completedTotal.coerceAtMost(session.totalSets),
                 completionSummary = logEntry,
                 isComplete = false
             )
         } else {
             _sessionState.value = currentState.copy(
                 completedSets = session.totalSets,
                 completionSummary = "Workout complete: ${session.title} finished.",
                 isComplete = true
             )
         }
     }

    private suspend fun buildDayUiFromPlanAsync(plan: WorkoutPlanEntity): List<WorkoutDayUi> {
         val sessions = repository.getSessionsForPlan(plan.id)
         return sessions.mapNotNull { session ->
             val sessionExercises = repository.getSessionExercises(session.id)
             val exerciseMap = repository.getAllExercises().associateBy { it.id }
            val mappedExercises = sessionExercises.mapNotNull { sessionExercise ->
                val exercise = exerciseMap[sessionExercise.exerciseId] ?: return@mapNotNull null
                WorkoutExerciseUi(
                    sessionExerciseId = sessionExercise.id,
                    exerciseId = exercise.id,
                    name = exercise.name,
                    targetMuscles = exercise.muscleGroup,
                    sets = sessionExercise.targetSets,
                    reps = sessionExercise.targetReps,
                    restSeconds = sessionExercise.targetRestSec,
                    notes = exercise.instructions
                )
            }
            if (mappedExercises.isEmpty()) return@mapNotNull null
            WorkoutDayUi(
                sessionId = session.id,
                dayName = dayOfWeekLabel(session.dayOfWeek),
                title = session.sessionName,
                focus = session.sessionName,
                estimatedMinutes = session.estimatedDurationMin,
                exerciseCount = mappedExercises.size,
                totalSets = mappedExercises.sumOf { it.sets },
                exercises = mappedExercises
            )
        }
    }

    private suspend fun generateWorkoutPlan(
        profile: UserProfileEntity,
        dietaryPreference: DietaryPreferenceEntity?
    ): Pair<WorkoutPlanEntity, List<WorkoutDayUi>> {
        val planId = UUID.randomUUID().toString()
        val splitType = determineSplitType(profile.daysPerWeek)
        val plan = WorkoutPlanEntity(
            id = planId,
            userId = userId,
            weekStartDate = System.currentTimeMillis(),
            splitType = splitType,
            status = "active",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val allExercises = repository.getAllExercises()
        val medicalFlags = parseMedicalFlags(dietaryPreference?.allergies)
        val allowedEquipment = equipmentFilter(profile.equipmentAccess)

        val sessionTemplates = when (splitType) {
            "3-Day Full Body" -> listOf(
                "Full Body A" to listOf("Chest", "Legs", "Core"),
                "Full Body B" to listOf("Back", "Legs", "Shoulders"),
                "Full Body C" to listOf("Chest", "Back", "Core")
            )
            "4-Day Upper / Lower" -> listOf(
                "Upper A" to listOf("Chest", "Back", "Shoulders"),
                "Lower A" to listOf("Legs", "Core"),
                "Upper B" to listOf("Back", "Shoulders", "Arms"),
                "Lower B" to listOf("Legs", "Core")
            )
            else -> listOf(
                "Push A" to listOf("Chest", "Shoulders"),
                "Pull A" to listOf("Back", "Arms"),
                "Legs A" to listOf("Legs", "Core"),
                "Push B" to listOf("Chest", "Shoulders"),
                "Pull B" to listOf("Back", "Arms"),
                "Legs B" to listOf("Legs", "Core")
            )
        }.take(profile.daysPerWeek.coerceIn(3, 6))

        val sessionRecords = mutableListOf<WorkoutSessionEntity>()
        val exerciseRecords = mutableListOf<SessionExerciseEntity>()
        val dayUi = mutableListOf<WorkoutDayUi>()

        sessionTemplates.forEachIndexed { index, pair ->
            val (sessionName, focusGroups) = pair
            val session = WorkoutSessionEntity(
                id = UUID.randomUUID().toString(),
                planId = planId,
                dayOfWeek = index + 1,
                sessionName = sessionName,
                estimatedDurationMin = profile.sessionMinutes.coerceAtLeast(30),
                estimatedCalories = estimateCalories(profile.goal, profile.sessionMinutes),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            sessionRecords.add(session)

            val chosenExercises = chooseExercisesForSession(
                profile = profile,
                allExercises = allExercises,
                medicalFlags = medicalFlags,
                allowedEquipment = allowedEquipment,
                focusGroups = focusGroups
            )

            val uiExercises = chosenExercises.mapIndexed { orderIndex, exercise ->
                val sessionExercise = SessionExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    exerciseId = exercise.id,
                    orderIndex = orderIndex,
                    targetSets = exercise.defaultSets,
                    targetReps = targetRepsForGoal(profile.goal, exercise),
                    targetRestSec = targetRestForGoal(profile.goal),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                exerciseRecords.add(sessionExercise)

                WorkoutExerciseUi(
                    sessionExerciseId = sessionExercise.id,
                    exerciseId = exercise.id,
                    name = exercise.name,
                    targetMuscles = exercise.muscleGroup,
                    sets = sessionExercise.targetSets,
                    reps = sessionExercise.targetReps,
                    restSeconds = sessionExercise.targetRestSec,
                    notes = exercise.instructions
                )
            }

            dayUi.add(
                WorkoutDayUi(
                    sessionId = session.id,
                    dayName = dayOfWeekLabel(index + 1),
                    title = sessionName,
                    focus = focusGroups.joinToString(", "),
                    estimatedMinutes = session.estimatedDurationMin,
                    exerciseCount = uiExercises.size,
                    totalSets = uiExercises.sumOf { it.sets },
                    exercises = uiExercises
                )
            )
        }

        repository.saveWorkoutPlanWithSessions(plan, sessionRecords, exerciseRecords)
        return plan to dayUi
    }

    private fun chooseExercisesForSession(
        profile: UserProfileEntity,
        allExercises: List<ExerciseLibraryEntity>,
        medicalFlags: Set<String>,
        allowedEquipment: Set<String>,
        focusGroups: List<String>
    ): List<ExerciseLibraryEntity> {
        val goalBias = when (profile.goal.lowercase()) {
            "weight loss" -> listOf("Core", "Legs", "Chest")
            "muscle gain" -> listOf("Chest", "Back", "Legs", "Arms", "Shoulders")
            "recomposition" -> listOf("Chest", "Back", "Legs", "Core")
            "endurance" -> listOf("Legs", "Core", "Shoulders")
            else -> listOf("Chest", "Back", "Legs", "Core")
        }

        val candidate = allExercises.filter { exercise ->
            val matchesEquipment = allowedEquipment.contains(exercise.equipmentTag.lowercase())
            val notRestricted = medicalFlags.none { flag ->
                exercise.exclusionTags?.contains(flag, ignoreCase = true) == true
            }
            val difficultyOk = when (profile.fitnessLevel.lowercase()) {
                "beginner" -> exercise.difficulty.lowercase() in setOf("beginner", "intermediate")
                "intermediate" -> exercise.difficulty.lowercase() in setOf("beginner", "intermediate", "advanced")
                else -> true
            }
            val focusMatch = focusGroups.any { it.lowercase() == exercise.muscleGroup.lowercase() }
            matchesEquipment && notRestricted && difficultyOk && (focusMatch || goalBias.any { it.equals(exercise.muscleGroup, ignoreCase = true) })
        }

        val sorted = candidate.sortedWith(compareBy<ExerciseLibraryEntity> {
            if (goalBias.contains(it.muscleGroup)) 0 else 1
        }.thenBy { it.difficulty.lowercase() != "beginner" })

        val desiredSize = if (profile.sessionMinutes <= 30) 4 else 5
        return sorted.take(desiredSize)
    }

    private fun parseMedicalFlags(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun determineSplitType(daysPerWeek: Int): String {
        return when (daysPerWeek) {
            3 -> "3-Day Full Body"
            4 -> "4-Day Upper / Lower"
            5, 6 -> "5-Day Push / Pull / Legs"
            else -> "3-Day Full Body"
        }
    }

    private fun equipmentFilter(raw: String): Set<String> {
        return when (raw.lowercase()) {
            "none", "bodyweight" -> setOf("bodyweight")
            "home basic" -> setOf("bodyweight", "dumbbell")
            else -> setOf("bodyweight", "dumbbell", "barbell")
        }
    }

    private fun targetRepsForGoal(goal: String, exercise: ExerciseLibraryEntity): Int {
        return when (goal.lowercase()) {
            "weight loss" -> if (exercise.muscleGroup.equals("Core", ignoreCase = true)) 15 else 12
            "muscle gain" -> 10
            "recomposition" -> 10
            "endurance" -> 15
            else -> 12
        }
    }

    private fun targetRestForGoal(goal: String): Int {
        return when (goal.lowercase()) {
            "weight loss" -> 45
            "muscle gain" -> 90
            "recomposition" -> 60
            "endurance" -> 45
            else -> 60
        }
    }

    private fun estimateCalories(goal: String, minutes: Int): Int {
        val base = when (goal.lowercase()) {
            "weight loss" -> 220
            "muscle gain" -> 280
            "recomposition" -> 260
            "endurance" -> 300
            else -> 240
        }
        return base + minutes
    }

    private fun dayOfWeekLabel(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> "Day $dayOfWeek"
        }
    }
}
