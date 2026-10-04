package com.example.kinetiq.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kinetiq.R
import com.example.kinetiq.databinding.FragmentWorkoutPlanBinding
import com.example.kinetiq.databinding.ItemWeeklyScheduleDayBinding
import com.example.kinetiq.databinding.ItemWorkoutPlanExerciseBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class WorkoutPlanFragment : Fragment() {

    private var _binding: FragmentWorkoutPlanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by viewModels()
    private var refreshWhenResumed = false
    private var selectedDayName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGeneratePlan.setOnClickListener {
            viewModel.generateWorkoutPlan(force = true)
        }

        setupRibbonClickListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                render(state)
            }
        }

        viewModel.loadWorkout()
    }

    override fun onPause() {
        super.onPause()
        refreshWhenResumed = true
    }

    override fun onResume() {
        super.onResume()
        if (refreshWhenResumed && _binding != null) {
            refreshWhenResumed = false
            viewModel.loadWorkout()
        }
    }

    private fun setupRibbonClickListeners() {
        val days = listOf(
            binding.btnDayMon to "Mon",
            binding.btnDayTue to "Tue",
            binding.btnDayWed to "Wed",
            binding.btnDayThu to "Thu",
            binding.btnDayFri to "Fri",
            binding.btnDaySat to "Sat",
            binding.btnDaySun to "Sun"
        )
        days.forEach { (view, dayName) ->
            view.setOnClickListener {
                selectedDayName = dayName
                render(viewModel.uiState.value)
            }
        }
    }

    private fun render(state: WorkoutUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.emptyStateGroup.visibility = if (!state.hasProfile) View.VISIBLE else View.GONE
        binding.planContentGroup.visibility = if (state.hasProfile) View.VISIBLE else View.GONE

        if (!state.hasProfile) {
            binding.tvEmptyState.text = state.profileMessage ?: "Complete your profile to generate your personalized workout plan."
            return
        }

        // Active split badge & header
        binding.tvWorkoutTitle.text = state.planTitle.ifBlank { "Weekly Workout Plan" }
        binding.tvWorkoutSummary.text = state.planSummary
        binding.tvSplitBadge.text = "⚡ ${state.planTitle.ifBlank { "ACTIVE SPLIT" }}"

        val todayCalendarDay = currentDayOfWeekLabel()

        // Determine which session is selected (defaults to today's workout or first available)
        val selectedSession = if (selectedDayName != null) {
            state.weeklyPlan.find { it.dayName.equals(selectedDayName, ignoreCase = true) }
                ?: state.todayWorkout
                ?: state.weeklyPlan.firstOrNull()
        } else {
            state.todayWorkout ?: state.weeklyPlan.firstOrNull()
        }

        if (selectedDayName == null && selectedSession != null) {
            selectedDayName = selectedSession.dayName
        }

        updateRibbonDisplay(state.weeklyPlan, todayCalendarDay, selectedSession?.dayName)

        val activeSession = selectedSession
        if (activeSession != null) {
            binding.cardTodayWorkout.visibility = View.VISIBLE
            val isToday = activeSession.dayName.equals(todayCalendarDay, ignoreCase = true)

            binding.tvSessionSectionBadge.text = if (isToday) {
                "TODAY'S SCHEDULED SESSION"
            } else {
                "${activeSession.dayName.uppercase()} SCHEDULED SESSION"
            }

            binding.tvTodayWorkoutTitle.text = activeSession.title
            binding.tvTodayWorkoutMeta.text = "${activeSession.dayName} · ${activeSession.exerciseCount} exercises · ${activeSession.totalSets} sets · ~${activeSession.estimatedMinutes} min"
            binding.tvWorkoutMinutes.text = "~${activeSession.estimatedMinutes} min"
            binding.tvExerciseCount.text = "${activeSession.exerciseCount} exercises"
            binding.tvWorkoutFocus.text = activeSession.focus.ifBlank { "Balanced" }

            binding.tvTodayWorkoutStatus.text = if (activeSession.isComplete) {
                "Session complete · ${activeSession.completedSets}/${activeSession.totalSets} sets"
            } else {
                "${activeSession.completedSets}/${activeSession.totalSets} sets completed"
            }

            binding.btnStartWorkout.text = if (activeSession.isComplete) {
                "▶  REPEAT WORKOUT"
            } else {
                "▶  START WORKOUT"
            }

            binding.btnStartWorkout.setOnClickListener {
                val args = Bundle().apply { putString("sessionId", activeSession.sessionId) }
                findNavController().navigate(R.id.action_workoutPlanFragment_to_workoutSessionFragment, args)
            }

            // Populate dynamic exercise cards matching Stitch design
            binding.llExerciseCardsContainer.removeAllViews()
            val inflater = LayoutInflater.from(requireContext())
            activeSession.exercises.forEachIndexed { index, exercise ->
                val itemBinding = ItemWorkoutPlanExerciseBinding.inflate(inflater, binding.llExerciseCardsContainer, false)
                itemBinding.tvExerciseNumber.text = (index + 1).toString()
                itemBinding.tvItemExerciseName.text = exercise.name
                itemBinding.tvItemExerciseMeta.text = "${exercise.targetMuscles} • ${exercise.sets} sets × ${exercise.reps} reps • Rest ${exercise.restSeconds}s"

                val isDone = exercise.completedSets >= exercise.sets
                val hasProgress = exercise.completedSets > 0 && !isDone
                when {
                    isDone -> {
                        itemBinding.tvItemExerciseStatus.text = "DONE"
                        itemBinding.tvItemExerciseStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_primary))
                        itemBinding.tvItemExerciseStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kinetic_card_high))
                    }
                    hasProgress -> {
                        itemBinding.tvItemExerciseStatus.text = "SET ${exercise.completedSets}/${exercise.sets}"
                        itemBinding.tvItemExerciseStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_secondary))
                        itemBinding.tvItemExerciseStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kinetic_card_high))
                    }
                    else -> {
                        itemBinding.tvItemExerciseStatus.text = "READY"
                        itemBinding.tvItemExerciseStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_primary))
                        itemBinding.tvItemExerciseStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kinetic_card_highest))
                    }
                }
                binding.llExerciseCardsContainer.addView(itemBinding.root)
            }

            // Preserved TextView update for backwards compatibility
            binding.tvTodayWorkoutExercises.text = activeSession.exercises.joinToString("\n") { exercise ->
                "${exercise.name} | ${exercise.targetMuscles} | ${exercise.sets} x ${exercise.reps} reps | Rest ${exercise.restSeconds}s"
            }
        } else {
            // Selected a rest day or no sessions
            binding.tvTodayWorkoutTitle.text = "Rest & Recovery"
            binding.tvTodayWorkoutMeta.text = "No intense resistance workout scheduled for ${selectedDayName ?: "today"}."
            binding.tvWorkoutMinutes.text = "Rest Day"
            binding.tvExerciseCount.text = "0 exercises"
            binding.tvWorkoutFocus.text = "Active Recovery & Mobility"
            binding.tvTodayWorkoutStatus.text = "Rest & Hydrate"
            binding.llExerciseCardsContainer.removeAllViews()
            binding.btnStartWorkout.text = "START ACTIVE RECOVERY"
            binding.btnStartWorkout.setOnClickListener {
                // If user wants to do recovery, find nearest session or open first available
                val fallbackSession = state.todayWorkout ?: state.weeklyPlan.firstOrNull()
                if (fallbackSession != null) {
                    val args = Bundle().apply { putString("sessionId", fallbackSession.sessionId) }
                    findNavController().navigate(R.id.action_workoutPlanFragment_to_workoutSessionFragment, args)
                }
            }
        }

        // Populate weekly breakdown schedule stack
        binding.llWeeklyScheduleContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        state.weeklyPlan.forEach { day ->
            val dayBinding = ItemWeeklyScheduleDayBinding.inflate(inflater, binding.llWeeklyScheduleContainer, false)
            dayBinding.tvScheduleDayBadge.text = day.dayName.uppercase()
            dayBinding.tvScheduleSessionTitle.text = day.title
            val statusText = if (day.isComplete) "Complete" else "${day.exerciseCount} exercises • ${day.totalSets} sets"
            dayBinding.tvScheduleStatus.text = statusText
            dayBinding.tvScheduleExercisesPreview.text = day.exercises.joinToString(" • ") { it.name }

            dayBinding.root.setOnClickListener {
                selectedDayName = day.dayName
                render(state)
            }
            binding.llWeeklyScheduleContainer.addView(dayBinding.root)
        }

        // Summary text for breakdown
        binding.tvScheduleCycleSummary.text = "${state.weeklyPlan.size} Workouts · ${7 - state.weeklyPlan.size} Rest"

        // Preserved TextView fallback
        val weekText = state.weeklyPlan.joinToString("\n\n") { day ->
            val status = if (day.isComplete) "Complete" else "${day.completedSets}/${day.totalSets} sets"
            val exercises = day.exercises.joinToString("\n") { exercise ->
                "  - ${exercise.name} | ${exercise.targetMuscles} | ${exercise.sets} x ${exercise.reps} reps | Rest ${exercise.restSeconds}s"
            }
            "${day.dayName} - ${day.title} (${day.exerciseCount} exercises | $status | ~${day.estimatedMinutes} min)\n$exercises"
        }
        binding.tvWeeklyPlan.text = weekText.ifBlank { "No workout plan generated yet." }
    }

    private fun updateRibbonDisplay(
        weeklyPlan: List<WorkoutDayUi>,
        todayLabel: String,
        selectedDay: String?
    ) {
        val ribbonViews = listOf(
            Triple(binding.btnDayMon, binding.tvDayMonLabel, binding.tvDayMonSub) to "Mon",
            Triple(binding.btnDayTue, binding.tvDayTueLabel, binding.tvDayTueSub) to "Tue",
            Triple(binding.btnDayWed, binding.tvDayWedLabel, binding.tvDayWedSub) to "Wed",
            Triple(binding.btnDayThu, binding.tvDayThuLabel, binding.tvDayThuSub) to "Thu",
            Triple(binding.btnDayFri, binding.tvDayFriLabel, binding.tvDayFriSub) to "Fri",
            Triple(binding.btnDaySat, binding.tvDaySatLabel, binding.tvDaySatSub) to "Sat",
            Triple(binding.btnDaySun, binding.tvDaySunLabel, binding.tvDaySunSub) to "Sun"
        )

        val planByDay = weeklyPlan.associateBy { it.dayName.lowercase() }

        ribbonViews.forEach { (viewTuple, dayName) ->
            val (container, labelView, subView) = viewTuple
            val isSelected = dayName.equals(selectedDay, ignoreCase = true)
            val isToday = dayName.equals(todayLabel, ignoreCase = true)
            val sessionForDay = planByDay[dayName.lowercase()]

            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_day_tab_active)
                labelView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_primary))
            } else {
                container.setBackgroundResource(R.drawable.bg_day_tab_normal)
                labelView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_muted))
            }

            when {
                sessionForDay != null && sessionForDay.isComplete -> {
                    subView.text = "DONE"
                    subView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_primary))
                }
                isToday -> {
                    subView.text = "TODAY"
                    subView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_primary))
                }
                sessionForDay != null -> {
                    subView.text = "PLAN"
                    subView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_secondary))
                }
                else -> {
                    subView.text = "REST"
                    subView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_muted))
                }
            }
        }
    }

    private fun currentDayOfWeekLabel(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> "Mon"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
