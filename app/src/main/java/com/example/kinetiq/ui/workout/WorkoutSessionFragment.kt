package com.example.kinetiq.ui.workout

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kinetiq.R
import com.example.kinetiq.databinding.FragmentWorkoutSessionBinding
import com.example.kinetiq.databinding.ItemWorkoutSetCompletedBinding
import com.example.kinetiq.databinding.ItemWorkoutSetPendingBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class WorkoutSessionFragment : Fragment() {

    private var _binding: FragmentWorkoutSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by viewModels()

    private var lastDisplayedExerciseIndex = -1
    private var currentReps = 10
    private var currentWeight = 0.0
    private var currentRpe = 8

    // Live session timer
    private var sessionElapsedSeconds = 0
    private var sessionTimerJob: Job? = null

    // Rest countdown timer
    private var restRemainingSeconds = 0
    private var restTimerJob: Job? = null

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

        setupSteppers()
        setupRpeButtons()
        setupActions()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collectLatest { state ->
                renderState(state)
            }
        }

        val sessionId = requireArguments().getString("sessionId")
        if (sessionId.isNullOrBlank()) {
            binding.btnCompleteExercise.isEnabled = false
            return
        }
        viewModel.loadSession(sessionId)
        startSessionTimer()
    }

    override fun onResume() {
        super.onResume()
        if (sessionTimerJob == null || sessionTimerJob?.isActive != true) {
            startSessionTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        restTimerJob?.cancel()
        restTimerJob = null
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val minutes = sessionElapsedSeconds / 60
                val seconds = sessionElapsedSeconds % 60
                binding.tvLiveWorkoutTimer.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                delay(1000L)
                sessionElapsedSeconds++
            }
        }
    }

    private fun startRestTimer(restSeconds: Int) {
        if (restSeconds <= 0) {
            binding.cardRestTimer.visibility = View.GONE
            return
        }
        restRemainingSeconds = restSeconds
        binding.cardRestTimer.visibility = View.VISIBLE
        updateRestTimerText()

        restTimerJob?.cancel()
        restTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && restRemainingSeconds > 0) {
                delay(1000L)
                restRemainingSeconds--
                updateRestTimerText()
            }
            binding.cardRestTimer.visibility = View.GONE
        }
    }

    private fun updateRestTimerText() {
        val minutes = restRemainingSeconds / 60
        val seconds = restRemainingSeconds % 60
        binding.tvRestCountdownText.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun setupSteppers() {
        // Reps Stepper
        binding.btnRepInc.setOnClickListener {
            currentReps = (currentReps + 1).coerceAtMost(500)
            updateRepsDisplay()
        }
        binding.btnRepDec.setOnClickListener {
            currentReps = (currentReps - 1).coerceAtLeast(1)
            updateRepsDisplay()
        }

        // Weight Stepper (increments of 2.5 kg)
        binding.btnWeightInc.setOnClickListener {
            currentWeight = (currentWeight + 2.5).coerceAtMost(1000.0)
            updateWeightDisplay()
        }
        binding.btnWeightDec.setOnClickListener {
            currentWeight = (currentWeight - 2.5).coerceAtLeast(0.0)
            updateWeightDisplay()
        }

        // Keep preserved EditTexts synchronized with steppers
        binding.etCompletedReps.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s?.toString()?.toIntOrNull()
                if (value != null && value in 1..500 && value != currentReps) {
                    currentReps = value
                    binding.tvRepVal.text = currentReps.toString()
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.etWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s?.toString()?.toDoubleOrNull()
                if (value != null && value >= 0.0 && value != currentWeight) {
                    currentWeight = value
                    binding.tvWeightVal.text = String.format(Locale.US, "%.1f", currentWeight)
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun updateRepsDisplay() {
        binding.tvRepVal.text = currentReps.toString()
        binding.etCompletedReps.setText(currentReps.toString())
    }

    private fun updateWeightDisplay() {
        binding.tvWeightVal.text = String.format(Locale.US, "%.1f", currentWeight)
        if (currentWeight > 0.0) {
            binding.etWeight.setText(String.format(Locale.US, "%.1f", currentWeight))
        } else {
            binding.etWeight.text?.clear()
        }
    }

    private fun setupRpeButtons() {
        val rpeButtons = listOf(
            binding.btnRpe7 to 7,
            binding.btnRpe8 to 8,
            binding.btnRpe9 to 9,
            binding.btnRpe10 to 10
        )
        rpeButtons.forEach { (button, rpe) ->
            button.setOnClickListener {
                currentRpe = rpe
                updateRpeHighlight(rpeButtons)
            }
        }
        updateRpeHighlight(rpeButtons)
    }

    private fun updateRpeHighlight(rpeButtons: List<Pair<Button, Int>>) {
        rpeButtons.forEach { (button, rpe) ->
            if (rpe == currentRpe) {
                button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kinetic_primary))
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            } else {
                button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kinetic_card_high))
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_primary))
            }
        }
    }

    private fun setupActions() {
        // Complete Set Action
        binding.btnCompleteExercise.setOnClickListener {
            val weightStr = if (currentWeight > 0.0) String.format(Locale.US, "%.1f", currentWeight) else ""
            viewModel.completeCurrentSet(
                weight = weightStr,
                reps = currentReps,
                rpe = currentRpe
            )

            // Trigger rest timer
            val state = viewModel.sessionState.value
            val nextSetIndex = state.completedSets + 1
            if (nextSetIndex < state.sets) {
                startRestTimer(state.restSeconds)
            }
        }

        // Rest timer controls
        binding.btnAddRest30s.setOnClickListener {
            restRemainingSeconds += 30
            updateRestTimerText()
        }
        binding.btnSkipRest.setOnClickListener {
            restTimerJob?.cancel()
            restTimerJob = null
            binding.cardRestTimer.visibility = View.GONE
        }

        // Skip Exercise Action
        binding.btnSkipExercise.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Skip Exercise")
                .setMessage("Are you sure you want to skip ${viewModel.sessionState.value.exerciseName}?")
                .setPositiveButton("Skip") { _, _ ->
                    viewModel.skipCurrentExercise()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // End Session Action
        binding.btnEndSession.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("End Workout Session")
                .setMessage("Do you want to end this workout session now?")
                .setPositiveButton("End Session") { _, _ ->
                    findNavController().navigateUp()
                }
                .setNegativeButton("Continue Workout", null)
                .show()
        }
    }

    private fun renderState(state: WorkoutSessionUiState) {
        // Live Header Bar
        binding.tvSessionName.text = state.sessionName.ifBlank { "Workout Session" }

        val exerciseNumber = state.currentExerciseIndex + 1
        val totalExercises = state.totalExercises.coerceAtLeast(1)
        binding.tvWorkoutSessionTitle.text = if (state.totalExercises > 0) {
            "Exercise $exerciseNumber of $totalExercises"
        } else {
            "Workout Complete"
        }

        // Progress calculation
        val percent = if (state.sessionSetsTotal > 0) {
            ((state.sessionCompletedSetsTotal.toFloat() / state.sessionSetsTotal) * 100).toInt()
        } else if (state.isComplete) 100 else 0

        binding.tvSessionPercentText.text = "$percent% Complete"
        binding.pbSessionProgress.progress = percent

        // Current Exercise Hero Card
        binding.tvExerciseName.text = state.exerciseName.ifBlank { "Session Complete" }
        binding.tvExerciseMuscles.text = state.targetMuscles.ifBlank { "Full Body" }.uppercase()
        binding.tvExerciseSets.text = "${state.sets} sets × ${state.reps} reps"
        binding.tvRestSeconds.text = "Rest: ${state.restSeconds} sec"
        binding.tvExerciseNotes.text = state.notes.ifBlank { "Maintain proper form and control throughout the movement." }

        // Overall Session Progress
        binding.tvProgress.text = "${state.sessionCompletedSetsTotal} / ${state.sessionSetsTotal} total sets completed ($percent%)"

        // Initialize steppers when switching to a new exercise
        if (state.hasWorkoutData && state.currentExerciseIndex != lastDisplayedExerciseIndex) {
            currentReps = state.reps.takeIf { it > 0 } ?: 10
            currentWeight = 0.0
            updateRepsDisplay()
            updateWeightDisplay()
            lastDisplayedExerciseIndex = state.currentExerciseIndex
        }

        // Set-by-Set Execution Log rendering
        renderSetsLog(state)

        // Button and completion states
        if (state.isComplete) {
            binding.tvCompletionStatus.visibility = View.VISIBLE
            binding.tvCompletionStatus.text = "🎉 ${state.completionSummary.ifBlank { "Workout Complete! Excellent job." }}"
            binding.cardActiveSet.visibility = View.GONE
            binding.cardRestTimer.visibility = View.GONE
            binding.llPendingSetsContainer.removeAllViews()
            binding.btnCompleteExercise.text = "WORKOUT FINISHED"
            binding.btnCompleteExercise.isEnabled = false
            binding.btnSkipExercise.visibility = View.GONE
        } else {
            binding.tvCompletionStatus.visibility = View.GONE
            binding.cardActiveSet.visibility = View.VISIBLE
            binding.btnCompleteExercise.isEnabled = state.hasWorkoutData && !state.isSaving
            val currentSetNumber = state.completedSets + 1
            binding.btnCompleteExercise.text = "✓  COMPLETE SET $currentSetNumber"
            binding.btnSkipExercise.visibility = View.VISIBLE
        }
    }

    private fun renderSetsLog(state: WorkoutSessionUiState) {
        val inflater = LayoutInflater.from(requireContext())

        // 1. Render completed sets
        binding.llCompletedSetsContainer.removeAllViews()
        state.loggedSetsHistory.forEach { detail ->
            val setBinding = ItemWorkoutSetCompletedBinding.inflate(inflater, binding.llCompletedSetsContainer, false)
            setBinding.tvCompletedSetTitle.text = "Set ${detail.setIndex}"
            setBinding.tvCompletedSetSub.text = "Target: ${state.reps} reps"
            setBinding.tvCompletedSetResult.text = detail.text
            setBinding.tvCompletedSetRpe.text = "RPE ${detail.rpe}.0"
            binding.llCompletedSetsContainer.addView(setBinding.root)
        }

        // 2. Active Set Card
        val activeSetNum = state.completedSets + 1
        binding.tvActiveSetTitle.text = "Active Set $activeSetNum"
        binding.tvActiveSetBadge.text = "SET $activeSetNum"
        binding.tvActiveSetPrescribed.text = "Prescribed: ${state.reps} reps · Rest ${state.restSeconds}s"

        // 3. Render pending sets
        binding.llPendingSetsContainer.removeAllViews()
        val remainingSets = state.sets - (state.completedSets + 1)
        if (remainingSets > 0) {
            for (i in 1..remainingSets) {
                val pendingSetNum = activeSetNum + i
                val pendingBinding = ItemWorkoutSetPendingBinding.inflate(inflater, binding.llPendingSetsContainer, false)
                pendingBinding.tvPendingSetNumber.text = pendingSetNum.toString()
                pendingBinding.tvPendingSetTitle.text = "Set $pendingSetNum"
                pendingBinding.tvPendingSetSub.text = "Target: ${state.reps} reps"
                binding.llPendingSetsContainer.addView(pendingBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        restTimerJob?.cancel()
        restTimerJob = null
        _binding = null
    }
}
