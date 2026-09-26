package com.example.kinetiq.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kinetiq.R
import com.example.kinetiq.databinding.FragmentWorkoutPlanBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutPlanFragment : Fragment() {

    private var _binding: FragmentWorkoutPlanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by viewModels()

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

        binding.btnStartWorkout.setOnClickListener {
            findNavController().navigate(R.id.action_workoutPlanFragment_to_workoutSessionFragment)
        }

        binding.btnGeneratePlan.setOnClickListener {
            viewModel.generateWorkoutPlan(force = true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                render(state)
            }
        }

        viewModel.loadWorkout()
    }

    private fun render(state: WorkoutUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.emptyStateGroup.visibility = if (!state.hasProfile) View.VISIBLE else View.GONE
        binding.planContentGroup.visibility = if (state.hasProfile) View.VISIBLE else View.GONE

        if (!state.hasProfile) {
            binding.tvEmptyState.text = state.profileMessage ?: "Complete your profile to generate your personalized workout plan."
            return
        }

        val today = state.todayWorkout
        val workoutMinutes = today?.estimatedMinutes ?: state.totalWorkoutMinutes
        val exerciseCount = today?.exerciseCount ?: state.totalExercises
        val totalSets = today?.totalSets ?: state.weeklyPlan.sumOf { it.totalSets }

        binding.tvWorkoutTitle.text = state.planTitle.ifBlank { "Workout" }
        binding.tvWorkoutSummary.text = state.planSummary
        binding.tvWorkoutMinutes.text = "~${workoutMinutes} min"
        binding.tvExerciseCount.text = "${exerciseCount} exercises • ${totalSets} sets"
        binding.tvWorkoutFocus.text = today?.focus ?: "Workout focus: Balanced"

        if (today != null) {
            binding.tvTodayWorkoutTitle.text = today.title
            binding.tvTodayWorkoutMeta.text = "${today.dayName} • ${today.exerciseCount} exercises • ${today.totalSets} sets • ~${today.estimatedMinutes} min"
        }

        val weekText = state.weeklyPlan.joinToString("\n") { day ->
            "${day.dayName} — ${day.title} (${day.exerciseCount} exercises • ${day.totalSets} sets • ~${day.estimatedMinutes} min)"
        }
        binding.tvWeeklyPlan.text = weekText.ifBlank { "No workout plan generated yet." }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}