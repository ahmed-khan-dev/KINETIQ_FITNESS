package com.example.kinetiq.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.kinetiq.databinding.FragmentProfileBinding
import com.example.kinetiq.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupListeners()
        viewModel.loadExistingProfile()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                renderState(state)
            }
        }
    }

    private fun setupSpinners() {
        val genders = listOf("Male", "Female", "Other")
        val goals = listOf("Weight Loss", "Muscle Gain", "Recomposition", "Endurance", "General Fitness")
        val fitnessLevels = listOf("Beginner", "Intermediate", "Advanced")
        val activityLevels = listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active")
        val equipmentOptions = listOf("None", "Home Basic", "Full Gym")
        val daysOptions = listOf(3, 4, 5, 6)
        val durationOptions = listOf(15, 30, 45, 60)
        val dietTypes = listOf("balanced", "vegetarian", "vegan", "non_vegetarian", "keto", "high_protein", "low_carb")

        binding.spGender.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)
        binding.spGoal.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goals)
        binding.spFitnessLevel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, fitnessLevels)
        binding.spActivityLevel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, activityLevels)
        binding.spEquipment.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, equipmentOptions)
        binding.spDaysPerWeek.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, daysOptions)
        binding.spSessionMinutes.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, durationOptions)
        binding.spDietType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, dietTypes)
    }

    private fun setupListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etAge.error = null
                binding.etHeightCm.error = null
                binding.etWeightKg.error = null
                collectAndSyncViewModelInput()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etAge.addTextChangedListener(textWatcher)
        binding.etHeightCm.addTextChangedListener(textWatcher)
        binding.etWeightKg.addTextChangedListener(textWatcher)
        binding.etAllergies.addTextChangedListener(textWatcher)
        binding.etMedicalFlags.addTextChangedListener(textWatcher)

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                collectAndSyncViewModelInput()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spGender.onItemSelectedListener = spinnerListener
        binding.spGoal.onItemSelectedListener = spinnerListener
        binding.spFitnessLevel.onItemSelectedListener = spinnerListener
        binding.spActivityLevel.onItemSelectedListener = spinnerListener
        binding.spEquipment.onItemSelectedListener = spinnerListener
        binding.spDaysPerWeek.onItemSelectedListener = spinnerListener
        binding.spSessionMinutes.onItemSelectedListener = spinnerListener
        binding.spDietType.onItemSelectedListener = spinnerListener

        binding.cbDisclaimer.setOnCheckedChangeListener { _, checked ->
            if (checked) binding.cbDisclaimer.error = null
            collectAndSyncViewModelInput()
        }

        binding.btnPreviousStep.setOnClickListener {
            viewModel.previousStep()
        }

        binding.btnNextStep.setOnClickListener {
            collectAndSyncViewModelInput()
            val currentState = viewModel.uiState.value
            if (currentState.currentStep < 4) {
                if (currentState.currentStep == 1 && !validateMetricFields()) return@setOnClickListener
                if (currentState.currentStep == 3 && !binding.cbDisclaimer.isChecked) {
                    binding.cbDisclaimer.error = "Please acknowledge this before continuing."
                    binding.cbDisclaimer.requestFocus()
                    return@setOnClickListener
                }
                viewModel.nextStep()
            } else {
                viewModel.saveFullProfile()
            }
        }

        binding.btnEditProfile.setOnClickListener {
            viewModel.setProfileMode(ProfileMode.Edit)
            viewModel.setStep(1)
        }
    }


    private fun collectAndSyncViewModelInput() {
        val age = binding.etAge.text.toString().toIntOrNull() ?: 0
        val gender = binding.spGender.selectedItem?.toString() ?: "Male"
        val heightCm = binding.etHeightCm.text.toString().toDoubleOrNull() ?: 0.0
        val weightKg = binding.etWeightKg.text.toString().toDoubleOrNull() ?: 0.0

        viewModel.updateMetrics(age, gender, heightCm, weightKg)

        val goal = binding.spGoal.selectedItem?.toString() ?: "Muscle Gain"
        val fitnessLevel = binding.spFitnessLevel.selectedItem?.toString() ?: "Intermediate"
        val activityLevel = binding.spActivityLevel.selectedItem?.toString() ?: "Moderate"
        val equipment = binding.spEquipment.selectedItem?.toString() ?: "Full Gym"
        val days = (binding.spDaysPerWeek.selectedItem as? Int) ?: 4
        val duration = (binding.spSessionMinutes.selectedItem as? Int) ?: 45

        viewModel.updateFitnessConfig(goal, fitnessLevel, activityLevel, equipment, days, duration)

        val dietType = binding.spDietType.selectedItem?.toString() ?: "balanced"
        val allergies = binding.etAllergies.text.toString()
        val medicalFlags = binding.etMedicalFlags.text.toString()
        val disclaimer = binding.cbDisclaimer.isChecked

        viewModel.updateNutritionAndHealth(dietType, allergies, medicalFlags, disclaimer)
    }

    private fun validateMetricFields(): Boolean {
        val age = binding.etAge.text.toString().trim().toIntOrNull()
        val height = binding.etHeightCm.text.toString().trim().toDoubleOrNull()
        val weight = binding.etWeightKg.text.toString().trim().toDoubleOrNull()
        binding.etAge.error = if (age == null || age !in 1..120) "Enter an age from 1 to 120." else null
        binding.etHeightCm.error = if (height == null || !height.isFinite() || height <= 0.0 || height > 300.0) "Enter a height above 0 and no more than 300 cm." else null
        binding.etWeightKg.error = if (weight == null || !weight.isFinite() || weight <= 0.0 || weight > 500.0) "Enter a weight above 0 and no more than 500 kg." else null
        val valid = binding.etAge.error == null && binding.etHeightCm.error == null && binding.etWeightKg.error == null
        if (!valid) {
            when {
                binding.etAge.error != null -> binding.etAge.requestFocus()
                binding.etHeightCm.error != null -> binding.etHeightCm.requestFocus()
                else -> binding.etWeightKg.requestFocus()
            }
        }
        return valid
    }

    private fun renderState(state: ProfileUiState) {
        val hasProfile = state.hasProfile || state.age > 0 || state.weightKg > 0.0

        if (state.mode == ProfileMode.Edit || state.mode == ProfileMode.Onboarding) {
            showEditorState(state)
            bindEditorState(state)
        } else {
            showSummaryState(hasProfile, state)
        }

        if (state.errorMessage != null) {
            binding.tvErrorMessage.text = state.errorMessage
            binding.tvErrorMessage.visibility = View.VISIBLE
        } else {
            binding.tvErrorMessage.visibility = View.GONE
        }

        if (state.isSavedSuccess) {
            Toast.makeText(requireContext(), "Profile & Fitness Targets Saved Successfully!", Toast.LENGTH_LONG).show()
            viewModel.markSaved()
        }
    }

    private fun showEditorState(state: ProfileUiState) {
        binding.headerProgressCard.visibility = View.VISIBLE
        binding.tvProfileTitle.text = if (state.mode == ProfileMode.Onboarding) "Onboarding" else "Edit Profile"
        binding.summaryContainer.visibility = View.GONE
        binding.editorContainer.visibility = View.GONE
        binding.step1Container.visibility = if (state.currentStep == 1) View.VISIBLE else View.GONE
        binding.step2Container.visibility = if (state.currentStep == 2) View.VISIBLE else View.GONE
        binding.step3Container.visibility = if (state.currentStep == 3) View.VISIBLE else View.GONE
        binding.step4Container.visibility = if (state.currentStep == 4) View.VISIBLE else View.GONE

        updateProgressUi(state.currentStep)

        binding.tvStepIndicator.visibility = View.VISIBLE
        binding.tvStepIndicator.text = when (state.currentStep) {
            1 -> "Step 1 of 4: Physical Metrics"
            2 -> "Step 2 of 4: Fitness & Goal Configuration"
            3 -> "Step 3 of 4: Nutrition & Health Disclaimer"
            4 -> "Step 4 of 4: Calculated Targets Summary"
            else -> ""
        }

        binding.btnPreviousStep.visibility = if (state.currentStep > 1) View.VISIBLE else View.GONE
        binding.btnNextStep.visibility = View.VISIBLE
        binding.btnNextStep.text = if (state.currentStep == 4) "Save Profile" else "Next Step"
    }

    private fun updateProgressUi(currentStep: Int) {
        val stepLabels = arrayOf(
            "Step 1 of 4: Physical Metrics",
            "Step 2 of 4: Fitness & Goal Configuration",
            "Step 3 of 4: Nutrition & Health Disclaimer",
            "Step 4 of 4: Calculated Targets Summary"
        )
        val badges = arrayOf(
            "25% Calibrated",
            "50% Calibrated",
            "75% Calibrated",
            "100% Calibrated"
        )

        binding.tvHeaderStepCounter.text = stepLabels.getOrElse(currentStep - 1) { stepLabels.last() }
        binding.tvHeaderCompletionBadge.text = badges.getOrElse(currentStep - 1) { badges.last() }

        val indicatorViews = listOf(
            binding.progressIndicator1,
            binding.progressIndicator2,
            binding.progressIndicator3,
            binding.progressIndicator4
        )

        indicatorViews.forEachIndexed { index, view ->
            val isActive = index < currentStep
            view.setBackgroundColor(
                if (isActive) resources.getColor(R.color.kinetic_primary, requireContext().theme)
                else resources.getColor(R.color.kinetic_card_highest, requireContext().theme)
            )
        }
    }

    private fun showSummaryState(hasProfile: Boolean, state: ProfileUiState) {
        binding.headerProgressCard.visibility = View.GONE
        binding.tvProfileTitle.text = "Profile"
        binding.summaryContainer.visibility = View.VISIBLE
        binding.editorContainer.visibility = View.GONE
        binding.step1Container.visibility = View.GONE
        binding.step2Container.visibility = View.GONE
        binding.step3Container.visibility = View.GONE
        binding.step4Container.visibility = View.GONE
        binding.tvStepIndicator.visibility = View.GONE
        binding.btnPreviousStep.visibility = View.GONE
        binding.btnNextStep.visibility = View.GONE

        if (hasProfile) {
            binding.tvSummaryTitle.text = "Profile Summary"
            binding.tvSummaryAge.text = "Age: ${state.age}"
            binding.tvSummaryGender.text = "Gender: ${state.gender}"
            binding.tvSummaryHeight.text = "Height: ${String.format(Locale.US, "%.0f", state.heightCm)} cm"
            binding.tvSummaryWeight.text = "Weight: ${String.format(Locale.US, "%.1f", state.weightKg)} kg"
            binding.tvSummaryGoal.text = "Goal: ${state.goal}"
            binding.tvSummaryFitness.text = "Fitness Level: ${state.fitnessLevel}"
            binding.tvSummaryActivity.text = "Activity Level: ${state.activityLevel}"
            binding.tvSummaryEquipment.text = "Equipment: ${state.equipmentAccess}"
            binding.tvSummarySession.text = "Session Duration: ${state.sessionMinutes} min"
            binding.tvSummaryDays.text = "Days/Week: ${state.daysPerWeek}"
            binding.tvSummaryDiet.text = "Diet: ${state.dietType}"
            binding.tvSummaryAllergies.text = "Allergies: ${state.allergies.ifBlank { "None" }}"
            binding.tvSummaryMedical.text = "Medical Restrictions: ${state.medicalFlags.ifBlank { "None" }}"
            binding.tvSummaryDisclaimer.text = "Medical Disclaimer: ${if (state.disclaimerAcknowledged) "Accepted" else "Not accepted"}"
            binding.tvSummaryBmr.text = "BMR: ${String.format(Locale.US, "%.0f", state.bmr)} kcal"
            binding.tvSummaryTdee.text = "TDEE: ${String.format(Locale.US, "%.0f", state.tdee)} kcal"
            binding.tvSummaryCalorie.text = "Calorie Target: ${state.calorieTarget} kcal"
            binding.tvSummaryProtein.text = "Protein: ${String.format(Locale.US, "%.0f", state.proteinG)}g"
            binding.tvSummaryCarbs.text = "Carbs: ${String.format(Locale.US, "%.0f", state.carbsG)}g"
            binding.tvSummaryFat.text = "Fat: ${String.format(Locale.US, "%.0f", state.fatG)}g"
        } else {
            binding.tvSummaryTitle.text = "Profile Not Set Up"
            binding.tvSummaryAge.text = "No saved profile yet."
            binding.tvSummaryGender.text = ""
            binding.tvSummaryHeight.text = ""
            binding.tvSummaryWeight.text = ""
            binding.tvSummaryGoal.text = ""
            binding.tvSummaryFitness.text = ""
            binding.tvSummaryActivity.text = ""
            binding.tvSummaryEquipment.text = ""
            binding.tvSummarySession.text = ""
            binding.tvSummaryDays.text = ""
            binding.tvSummaryDiet.text = ""
            binding.tvSummaryAllergies.text = ""
            binding.tvSummaryMedical.text = ""
            binding.tvSummaryDisclaimer.text = ""
            binding.tvSummaryBmr.text = ""
            binding.tvSummaryTdee.text = ""
            binding.tvSummaryCalorie.text = ""
            binding.tvSummaryProtein.text = ""
            binding.tvSummaryCarbs.text = ""
            binding.tvSummaryFat.text = ""
        }
    }

    private fun bindEditorState(state: ProfileUiState) {
        val weightKg = state.weightKg.coerceIn(0.0, 500.0)
        if (binding.etAge.text?.toString() != state.age.toString()) {
            binding.etAge.setText(state.age.toString())
        }
        if (binding.etHeightCm.text?.toString() != state.heightCm.toString()) {
            binding.etHeightCm.setText(state.heightCm.toString())
        }
        if (binding.etWeightKg.text?.toString() != weightKg.toString()) {
            binding.etWeightKg.setText(String.format(Locale.US, "%.1f", weightKg))
        }
        if (binding.etAllergies.text?.toString() != state.allergies) {
            binding.etAllergies.setText(state.allergies)
        }
        if (binding.etMedicalFlags.text?.toString() != state.medicalFlags) {
            binding.etMedicalFlags.setText(state.medicalFlags)
        }
        binding.cbDisclaimer.isChecked = state.disclaimerAcknowledged

        selectSpinnerValue(binding.spGender, state.gender, listOf("Male", "Female", "Other"))
        selectSpinnerValue(binding.spGoal, state.goal, listOf("Weight Loss", "Muscle Gain", "Recomposition", "Endurance", "General Fitness"))
        selectSpinnerValue(binding.spFitnessLevel, state.fitnessLevel, listOf("Beginner", "Intermediate", "Advanced"))
        selectSpinnerValue(binding.spActivityLevel, state.activityLevel, listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active"))
        selectSpinnerValue(binding.spEquipment, state.equipmentAccess, listOf("None", "Home Basic", "Full Gym"))
        selectSpinnerValue(binding.spDaysPerWeek, state.daysPerWeek, listOf(3, 4, 5, 6))
        selectSpinnerValue(binding.spSessionMinutes, state.sessionMinutes, listOf(15, 30, 45, 60))
        selectSpinnerValue(binding.spDietType, state.dietType, listOf("balanced", "vegetarian", "vegan", "non_vegetarian", "keto", "high_protein", "low_carb"))
    }

    private fun <T> selectSpinnerValue(spinner: android.widget.Spinner, value: T, options: List<T>) {
        val index = options.indexOfFirst { it == value }
        if (index >= 0 && spinner.selectedItemPosition != index) {
            spinner.setSelection(index)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
