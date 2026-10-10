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
import com.example.kinetiq.data.local.entity.StreakEntity
import com.example.kinetiq.data.local.entity.UserProfileEntity
import com.example.kinetiq.data.local.entity.WorkoutPlanEntity
import com.example.kinetiq.data.local.entity.WorkoutSessionEntity
import com.example.kinetiq.utils.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Calendar
import kotlin.math.max

data class WorkoutDayUi(
    val sessionId: String,
    val dayName: String,
    val title: String,
    val focus: String,
    val estimatedMinutes: Int,
    val exerciseCount: Int,
    val totalSets: Int,
    val exercises: List<WorkoutExerciseUi>,
    val completedSets: Int = 0,
    val isComplete: Boolean = false
)

data class WorkoutExerciseUi(
    val sessionExerciseId: String,
    val exerciseId: String,
    val name: String,
    val targetMuscles: String,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val notes: String,
    val completedSets: Int = 0
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

data class LoggedSetDetail(
    val setIndex: Int,
    val text: String,
    val rpe: Int = 8
)

data class WorkoutSessionUiState(
    val hasWorkoutData: Boolean = false,
    val isComplete: Boolean = false,
    val isSaving: Boolean = false,
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
    val currentSessionId: String = "",
    val loggedSetsHistory: List<LoggedSetDetail> = emptyList(),
    val sessionCompletedSetsTotal: Int = 0,
    val sessionSetsTotal: Int = 0
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
    private var activeSessionExercises: List<WorkoutExerciseUi> = emptyList()
    private var completedSetsByExercise: Map<String, Int> = emptyMap()

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
                selectedSessionIndex = findTodayOrNextSessionIndex(mapped)
                val todaySession = mapped.getOrNull(selectedSessionIndex) ?: mapped.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasProfile = true,
                    profileMessage = null,
                    activePlanId = activePlan.id,
                    planTitle = activePlan.splitType,
                    planSummary = "${mapped.size} workout sessions planned for this week",
                    weeklyPlan = mapped,
                    todayWorkout = todaySession,
                    totalWorkoutMinutes = mapped.sumOf { it.estimatedMinutes },
                    totalExercises = mapped.sumOf { it.exerciseCount },
                    errorMessage = null
                )
                prepareSession(selectedSessionIndex)
                return@launch
            }

            val generatedPlan = generateWorkoutPlan(profile, dietaryPreference)
            cachedSessions = generatedPlan.second
            selectedSessionIndex = findTodayOrNextSessionIndex(cachedSessions)
            val todaySession = cachedSessions.getOrNull(selectedSessionIndex) ?: cachedSessions.firstOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasProfile = true,
                profileMessage = null,
                activePlanId = generatedPlan.first.id,
                planTitle = generatedPlan.first.splitType,
                planSummary = "${generatedPlan.second.size} workout sessions planned for this week",
                weeklyPlan = generatedPlan.second,
                todayWorkout = todaySession,
                totalWorkoutMinutes = generatedPlan.second.sumOf { it.estimatedMinutes },
                totalExercises = generatedPlan.second.sumOf { it.exerciseCount },
                errorMessage = null
            )
            prepareSession(selectedSessionIndex)
        }
    }

    fun addCustomExercise(
        name: String,
        muscleGroup: String = "General",
        equipment: String = "Bodyweight"
    ) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            repository.saveCustomExercise(name, muscleGroup, equipment)
            loadWorkout(forceGenerate = false)
        }
    }

    fun addCustomExerciseToSession(
        sessionId: String,
        exerciseName: String,
        muscleGroup: String = "Chest",
        targetSets: Int = 3,
        targetReps: Int = 10
    ) {
        viewModelScope.launch {
            if (exerciseName.isBlank()) return@launch
            val exercise = repository.saveCustomExercise(exerciseName, muscleGroup, "Dumbbell")
            val sessionExercise = com.example.kinetiq.data.local.entity.SessionExerciseEntity(
                id = java.util.UUID.randomUUID().toString(),
                sessionId = sessionId,
                exerciseId = exercise.id,
                orderIndex = 99,
                targetSets = targetSets,
                targetReps = targetReps,
                targetRestSec = 60,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.workoutPlanDao.insertSessionExercises(listOf(sessionExercise))
            loadWorkout(forceGenerate = false)
        }
    }

    fun addCustomExerciseToDay(
        dayName: String,
        exerciseName: String,
        muscleGroup: String = "Chest",
        targetSets: Int = 3,
        targetReps: Int = 10
    ) {
        viewModelScope.launch {
            if (exerciseName.isBlank()) return@launch
            val activePlan = repository.getActiveWorkoutPlan(userId) ?: return@launch
            val dayOfWeek = dayNameLabelToOfWeek(dayName)
            val existingSessions = repository.getSessionsForPlan(activePlan.id)
            var session = existingSessions.find { it.dayOfWeek == dayOfWeek }

            if (session == null) {
                val newSessionId = java.util.UUID.randomUUID().toString()
                session = com.example.kinetiq.data.local.entity.WorkoutSessionEntity(
                    id = newSessionId,
                    planId = activePlan.id,
                    dayOfWeek = dayOfWeek,
                    sessionName = "$dayName Workout",
                    estimatedDurationMin = 45,
                    estimatedCalories = 300,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.workoutPlanDao.insertWorkoutSessions(listOf(session))
            }

            val exercise = repository.saveCustomExercise(exerciseName, muscleGroup, "Dumbbell")
            val sessionExercise = com.example.kinetiq.data.local.entity.SessionExerciseEntity(
                id = java.util.UUID.randomUUID().toString(),
                sessionId = session.id,
                exerciseId = exercise.id,
                orderIndex = 99,
                targetSets = targetSets,
                targetReps = targetReps,
                targetRestSec = 60,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.workoutPlanDao.insertSessionExercises(listOf(sessionExercise))
            loadWorkout(forceGenerate = false)
        }
    }

    private fun dayNameLabelToOfWeek(dayName: String): Int = when (dayName.lowercase()) {
        "mon" -> 1
        "tue" -> 2
        "wed" -> 3
        "thu" -> 4
        "fri" -> 5
        "sat" -> 6
        "sun" -> 7
        else -> 1
    }

    fun deleteSessionExercise(sessionExerciseId: String) {
        viewModelScope.launch {
            repository.deleteSessionExercise(sessionExerciseId)
            loadWorkout(forceGenerate = false)
        }
    }

    fun updateSessionExercise(sessionExerciseId: String, sets: Int, reps: Int, restSec: Int) {
        viewModelScope.launch {
            repository.updateSessionExercise(sessionExerciseId, sets, reps, restSec)
            loadWorkout(forceGenerate = false)
        }
    }

    fun updateSessionName(sessionId: String, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.updateSessionName(sessionId, newName.trim())
                loadWorkout(forceGenerate = false)
            }
        }
    }

    fun startNewWeekCycle() {
        viewModelScope.launch {
            val activePlan = repository.getActiveWorkoutPlan(userId) ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.repeatWorkoutPlanNewWeek(activePlan)
            loadWorkout(forceGenerate = false)
        }
    }

    private fun findTodayOrNextSessionIndex(mapped: List<WorkoutDayUi>): Int {
        if (mapped.isEmpty()) return 0
        val currentDay = currentDayOfWeekIndex()
        val todayMatchIndex = mapped.indexOfFirst { day ->
            day.dayName.equals(dayOfWeekLabel(currentDay), ignoreCase = true)
        }
        if (todayMatchIndex >= 0) return todayMatchIndex

        val incompleteIndex = mapped.indexOfFirst { !it.isComplete }
        return if (incompleteIndex >= 0) incompleteIndex else 0
    }

    fun generateWorkoutPlan(force: Boolean = false) {
        loadWorkout(forceGenerate = force)
    }

    /** Loads the selected, persisted workout session and its linked exercise records. */
    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val session = repository.getWorkoutSession(sessionId)
            if (session == null) {
                activeSessionExercises = emptyList()
                completedSetsByExercise = emptyMap()
                _sessionState.value = WorkoutSessionUiState()
                return@launch
            }

            val exerciseMap = repository.getAllExercises().associateBy { it.id }
            activeSessionExercises = repository.getSessionExercises(sessionId).mapNotNull { sessionExercise ->
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
            completedSetsByExercise = activeSessionExercises.associate { exercise ->
                val savedLogs = repository.getExerciseLogsForSessionExercise(exercise.sessionExerciseId)
                exercise.sessionExerciseId to savedLogs.sumOf { log ->
                    log.loggedSets.substringBefore('x').trim().toIntOrNull() ?: 1
                }
            }

            val nextExerciseIndex = activeSessionExercises.indexOfFirst { exercise ->
                (completedSetsByExercise[exercise.sessionExerciseId] ?: 0) < exercise.sets
            }.let { if (it < 0) activeSessionExercises.size else it }
            updateSessionStateAsync(session.sessionName, sessionId, nextExerciseIndex)
        }
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
            currentSessionId = session.sessionId,
            sessionSetsTotal = sessionTotalSets,
            sessionCompletedSetsTotal = 0
        )
    }

    fun completeCurrentSet(weight: String = "", reps: Int, rpe: Int = 8) {
        val currentState = _sessionState.value
        if (!currentState.hasWorkoutData || currentState.isSaving || currentState.isComplete) return
        val exercise = activeSessionExercises.getOrNull(currentState.currentExerciseIndex) ?: return
        if (reps !in 1..500) return
        val weightValue = weight.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (weight.isNotBlank() && (weightValue == null || !weightValue.isFinite() || weightValue <= 0.0 || weightValue > 1000.0)) return

        _sessionState.value = currentState.copy(isSaving = true)
        viewModelScope.launch {
            val gpsLog = LocationUtils.getCurrentLocation(getApplication())
            repository.saveGpsLog(gpsLog)
            val weightText = weight.trim().takeIf { it.isNotBlank() }?.let { " @ $it kg" } ?: ""
            repository.saveExerciseLog(
                ExerciseLogEntity(
                    id = UUID.randomUUID().toString(),
                    sessionExerciseId = exercise.sessionExerciseId,
                    userId = userId,
                    loggedSets = "1 x $reps$weightText",
                    rpe = rpe.coerceIn(1, 10),
                    completedAt = System.currentTimeMillis(),
                    gpsLogId = gpsLog.id
                )
            )

            completedSetsByExercise = completedSetsByExercise + (
                exercise.sessionExerciseId to ((completedSetsByExercise[exercise.sessionExerciseId] ?: 0) + 1)
            )
            val nextIndex = if ((completedSetsByExercise[exercise.sessionExerciseId] ?: 0) >= exercise.sets) {
                currentState.currentExerciseIndex + 1
            } else {
                currentState.currentExerciseIndex
            }
            updateSessionStateAsync(
                sessionName = currentState.sessionName,
                sessionId = currentState.currentSessionId,
                exerciseIndex = nextIndex,
                completionSummary = "Saved set: 1 x $reps$weightText"
            )
            if (nextIndex >= activeSessionExercises.size) {
                recordCompletedWorkoutForStreak()
            }
        }
    }

    fun skipCurrentExercise() {
        val currentState = _sessionState.value
        if (!currentState.hasWorkoutData || currentState.isComplete) return
        val nextIndex = currentState.currentExerciseIndex + 1
        viewModelScope.launch {
            updateSessionStateAsync(
                sessionName = currentState.sessionName,
                sessionId = currentState.currentSessionId,
                exerciseIndex = nextIndex,
                completionSummary = "Skipped ${currentState.exerciseName}."
            )
            if (nextIndex >= activeSessionExercises.size) {
                recordCompletedWorkoutForStreak()
            }
        }
    }

    private suspend fun recordCompletedWorkoutForStreak() {
        val now = System.currentTimeMillis()
        val today = startOfLocalDay(now)
        val existing = repository.streakDao.getStreak(userId)
        val previousActiveDay = existing?.let { startOfLocalDay(it.lastActiveDate) }
        if (previousActiveDay == today) return

        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val current = if (previousActiveDay == yesterday) (existing?.currentStreak ?: 0) + 1 else 1
        val streak = existing?.copy(
            currentStreak = current,
            longestStreak = max(existing.longestStreak, current),
            lastActiveDate = today,
            updatedAt = now
        ) ?: StreakEntity(
            userId = userId,
            currentStreak = current,
            longestStreak = current,
            lastActiveDate = today,
            createdAt = now,
            updatedAt = now
        )
        repository.saveStreak(streak)
    }

    private fun startOfLocalDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private suspend fun updateSessionStateAsync(
        sessionName: String,
        sessionId: String,
        exerciseIndex: Int,
        completionSummary: String = ""
    ) {
        val totalSessionSets = activeSessionExercises.sumOf { it.sets }
        val completedSessionSets = activeSessionExercises.sumOf { exercise ->
            (completedSetsByExercise[exercise.sessionExerciseId] ?: 0).coerceAtMost(exercise.sets)
        }
        val exercise = activeSessionExercises.getOrNull(exerciseIndex)
        if (exercise == null) {
            _sessionState.value = WorkoutSessionUiState(
                hasWorkoutData = activeSessionExercises.isNotEmpty(),
                isComplete = activeSessionExercises.isNotEmpty(),
                sessionName = sessionName,
                totalSessions = 1,
                totalExercises = activeSessionExercises.size,
                totalSets = totalSessionSets,
                completedSets = completedSessionSets,
                completionSummary = completionSummary.ifBlank { "Workout complete: $sessionName finished." },
                currentSessionId = sessionId,
                sessionCompletedSetsTotal = completedSessionSets,
                sessionSetsTotal = totalSessionSets
            )
            return
        }

        val logs = repository.getExerciseLogsForSessionExercise(exercise.sessionExerciseId)
        val history = logs.mapIndexed { idx, log ->
            val setNumber = idx + 1
            val cleanText = log.loggedSets.removePrefix("1 x ").trim()
            LoggedSetDetail(
                setIndex = setNumber,
                text = "${exercise.reps} reps${if (cleanText.contains("@")) " • " + cleanText.substringAfter("@").trim() else ""}",
                rpe = log.rpe ?: 8
            )
        }

        val exerciseCompletedSets = (completedSetsByExercise[exercise.sessionExerciseId] ?: 0).coerceAtMost(exercise.sets)

        _sessionState.value = WorkoutSessionUiState(
            hasWorkoutData = true,
            isSaving = false,
            sessionName = sessionName,
            totalSessions = 1,
            exerciseName = exercise.name,
            currentExerciseIndex = exerciseIndex,
            totalExercises = activeSessionExercises.size,
            targetMuscles = exercise.targetMuscles,
            sets = exercise.sets,
            reps = exercise.reps,
            totalSets = exercise.sets,
            completedSets = exerciseCompletedSets,
            restSeconds = exercise.restSeconds,
            notes = exercise.notes,
            sessionExerciseId = exercise.sessionExerciseId,
            completionSummary = completionSummary,
            currentSessionId = sessionId,
            loggedSetsHistory = history,
            sessionCompletedSetsTotal = completedSessionSets,
            sessionSetsTotal = totalSessionSets
        )
    }

    private suspend fun buildDayUiFromPlanAsync(plan: WorkoutPlanEntity): List<WorkoutDayUi> {
         val sessions = repository.getSessionsForPlan(plan.id)
         return sessions.mapNotNull { session ->
             val sessionExercises = repository.getSessionExercises(session.id)
             val exerciseMap = repository.getAllExercises().associateBy { it.id }
            val mappedExercises = sessionExercises.mapNotNull { sessionExercise ->
                val exercise = exerciseMap[sessionExercise.exerciseId] ?: return@mapNotNull null
                val completedSetCount = repository.getExerciseLogsForSessionExercise(sessionExercise.id)
                    .sumOf(::completedSetCountFromLog)
                WorkoutExerciseUi(
                    sessionExerciseId = sessionExercise.id,
                    exerciseId = exercise.id,
                    name = exercise.name,
                    targetMuscles = exercise.muscleGroup,
                    sets = sessionExercise.targetSets,
                    reps = sessionExercise.targetReps,
                    restSeconds = sessionExercise.targetRestSec,
                    notes = exercise.instructions,
                    completedSets = completedSetCount.coerceAtMost(sessionExercise.targetSets)
                )
            }
            WorkoutDayUi(
                sessionId = session.id,
                dayName = dayOfWeekLabel(session.dayOfWeek),
                title = session.sessionName,
                focus = session.sessionName,
                estimatedMinutes = session.estimatedDurationMin,
                exerciseCount = mappedExercises.size,
                totalSets = mappedExercises.sumOf { it.sets },
                exercises = mappedExercises,
                completedSets = mappedExercises.sumOf { it.completedSets },
                isComplete = mappedExercises.isNotEmpty() && mappedExercises.all { it.completedSets >= it.sets }
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

        val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        (1..7).forEach { dayIndex ->
            val templateIndex = dayIndex - 1
            val dayLabel = daysList[templateIndex]
            val template = sessionTemplates.getOrNull(templateIndex)
            val sessionName = template?.first ?: "$dayLabel Workout"
            val focusGroups = template?.second ?: listOf("General")

            val session = WorkoutSessionEntity(
                id = UUID.randomUUID().toString(),
                planId = planId,
                dayOfWeek = dayIndex,
                sessionName = sessionName,
                estimatedDurationMin = profile.sessionMinutes.coerceAtLeast(30),
                estimatedCalories = estimateCalories(profile.goal, profile.sessionMinutes),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            sessionRecords.add(session)

            val uiExercises = if (template != null) {
                val chosenExercises = chooseExercisesForSession(
                    profile = profile,
                    allExercises = allExercises,
                    medicalFlags = medicalFlags,
                    allowedEquipment = allowedEquipment,
                    focusGroups = focusGroups
                )

                chosenExercises.mapIndexed { orderIndex, exercise ->
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
            } else {
                emptyList()
            }

            dayUi.add(
                WorkoutDayUi(
                    sessionId = session.id,
                    dayName = dayLabel,
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

    private fun currentDayOfWeekIndex(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    private fun completedSetCountFromLog(log: ExerciseLogEntity): Int =
        log.loggedSets.substringBefore('x').trim().toIntOrNull() ?: 1
}
