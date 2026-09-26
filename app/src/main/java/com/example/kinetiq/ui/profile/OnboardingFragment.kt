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
import androidx.navigation.fragment.findNavController
import com.example.kinetiq.R
import com.example.kinetiq.databinding.FragmentOnboardingBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupListeners()

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
        if (state.errorMessage != null) {
            binding.tvErrorMessage.text = state.errorMessage
            binding.tvErrorMessage.visibility = View.VISIBLE
        } else {
            binding.tvErrorMessage.visibility = View.GONE
        }

        if (state.isSavedSuccess) {
            Toast.makeText(requireContext(), "Profile setup complete!", Toast.LENGTH_SHORT).show()
            if (findNavController().currentDestination?.id == R.id.onboardingFragment) {
                findNavController().navigate(R.id.action_onboardingFragment_to_homeFragment)
            }
            return
        }

        binding.step1Container.visibility = if (state.currentStep == 1) View.VISIBLE else View.GONE
        binding.step2Container.visibility = if (state.currentStep == 2) View.VISIBLE else View.GONE
        binding.step3Container.visibility = if (state.currentStep == 3) View.VISIBLE else View.GONE
        binding.step4Container.visibility = if (state.currentStep == 4) View.VISIBLE else View.GONE

        binding.tvStepIndicator.text = when (state.currentStep) {
            1 -> "Step 1 of 4: Physical Metrics"
            2 -> "Step 2 of 4: Fitness & Goal Configuration"
            3 -> "Step 3 of 4: Nutrition & Health Disclaimer"
            4 -> "Step 4 of 4: Calculated Targets Summary"
            else -> ""
        }

        binding.btnPreviousStep.visibility = if (state.currentStep > 1) View.VISIBLE else View.GONE
        binding.btnNextStep.text = if (state.currentStep == 4) "Finish & Save Profile" else "Next Step"

        binding.tvBmr.text = "BMR: ${String.format("%.0f", state.bmr)} kcal"
        binding.tvTdee.text = "TDEE: ${String.format("%.0f", state.tdee)} kcal"
        binding.tvCalorieTarget.text = "Daily Calorie Goal: ${state.calorieTarget} kcal"

        val proteinCal = (state.proteinG * 4).toInt()
        binding.tvProteinTarget.text = "Protein: ${String.format("%.0f", state.proteinG)}g ($proteinCal kcal)"

        val fatCal = (state.fatG * 9).toInt()
        binding.tvFatTarget.text = "Fat: ${String.format("%.0f", state.fatG)}g ($fatCal kcal)"

        val carbsCal = (state.carbsG * 4).toInt()
        binding.tvCarbsTarget.text = "Carbohydrates: ${String.format("%.0f", state.carbsG)}g ($carbsCal kcal)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
