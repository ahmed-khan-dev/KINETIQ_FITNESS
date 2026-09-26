package com.example.kinetiq.ui.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private var lastDisplayedExerciseIndex = -1

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
            val reps = binding.etCompletedReps.text.toString().trim().toIntOrNull()
            val weightText = binding.etWeight.text.toString().trim()
            val weight = weightText.takeIf(String::isNotEmpty)?.toDoubleOrNull()
            binding.etCompletedReps.error = if (reps == null || reps !in 1..500) "Enter reps from 1 to 500." else null
            binding.etWeight.error = if (weightText.isNotEmpty() && (weight == null || !weight.isFinite() || weight <= 0.0 || weight > 1000.0)) {
                "Enter a weight above 0 and no more than 1,000 kg, or leave it blank."
            } else null
            if (binding.etCompletedReps.error != null || binding.etWeight.error != null) {
                if (binding.etCompletedReps.error != null) binding.etCompletedReps.requestFocus() else binding.etWeight.requestFocus()
                return@setOnClickListener
            }
            viewModel.completeCurrentSet(
                weight = weightText,
                reps = reps!!
            )
        }

        listOf(binding.etCompletedReps, binding.etWeight).forEach { field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { field.error = null }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collectLatest { state ->
                renderState(state)
            }
        }

        binding.btnCompleteExercise.text = "Complete Set"
        binding.etCompletedSets.visibility = View.GONE
        val sessionId = requireArguments().getString("sessionId")
        if (sessionId.isNullOrBlank()) {
            binding.btnCompleteExercise.isEnabled = false
            return
        }
        viewModel.loadSession(sessionId)
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
        binding.btnCompleteExercise.isEnabled = state.hasWorkoutData && !state.isComplete && !state.isSaving

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
        if (state.hasWorkoutData && state.currentExerciseIndex != lastDisplayedExerciseIndex) {
            binding.etCompletedReps.setText(state.reps.takeIf { it > 0 }?.toString().orEmpty())
            binding.etWeight.text?.clear()
            binding.etCompletedReps.error = null
            binding.etWeight.error = null
            lastDisplayedExerciseIndex = state.currentExerciseIndex
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
