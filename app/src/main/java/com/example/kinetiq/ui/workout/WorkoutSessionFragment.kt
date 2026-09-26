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
import com.example.kinetiq.databinding.FragmentWorkoutSessionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutSessionFragment : Fragment() {

    private var _binding: FragmentWorkoutSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCompleteExercise.setOnClickListener {
            val reps = binding.etCompletedReps.text.toString().toIntOrNull() ?: 0
            val sets = binding.etCompletedSets.text.toString().toIntOrNull() ?: 0
            viewModel.completeCurrentExercise(
                weight = binding.etWeight.text.toString(),
                repsOverride = reps.takeIf { it > 0 },
                setsOverride = sets.takeIf { it > 0 }
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collectLatest { state ->
                renderState(state)
            }
        }

        viewModel.prepareSession(0)
    }

    private fun renderState(state: WorkoutSessionUiState) {
        val title = if (state.totalSessions > 0) {
            "Exercise ${state.currentExerciseIndex + 1} of ${state.totalExercises}"
        } else {
            "Exercise"
        }
        binding.tvWorkoutSessionTitle.text = title
        binding.tvSessionName.text = state.sessionName
        binding.tvExerciseName.text = state.exerciseName
        binding.tvExerciseMuscles.text = state.targetMuscles.ifBlank { "Target muscles: not specified" }
        binding.tvExerciseSets.text = "${state.sets} sets × ${state.reps} reps"
        binding.tvRestSeconds.text = "Rest: ${state.restSeconds} sec"
        binding.tvExerciseNotes.text = state.notes.ifBlank { "No exercise notes available." }
        binding.tvProgress.text = "${state.completedSets} / ${state.totalSets} sets completed"

        if (state.isComplete) {
            binding.tvCompletionStatus.visibility = View.VISIBLE
            binding.tvCompletionStatus.text = state.completionSummary
            binding.btnCompleteExercise.text = "Workout Complete"
            binding.btnCompleteExercise.isEnabled = false
        } else {
            binding.tvCompletionStatus.visibility = View.GONE
            binding.btnCompleteExercise.text = "Complete Exercise"
            binding.btnCompleteExercise.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
