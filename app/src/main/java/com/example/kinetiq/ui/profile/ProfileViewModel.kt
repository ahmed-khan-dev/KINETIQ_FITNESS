package com.example.kinetiq.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetiq.KinetiqApplication
import com.example.kinetiq.data.local.entity.DietaryPreferenceEntity
import com.example.kinetiq.data.local.entity.UserProfileEntity
import com.example.kinetiq.data.local.entity.UserTargetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProfileMode {
    Onboarding,
    View,
    Edit
}

data class ProfileUiState(
    val currentStep: Int = 1,
    val age: Int = 25,
    val gender: String = "Male",
    val heightCm: Double = 175.0,
    val weightKg: Double = 70.0,
    val goal: String = "Muscle Gain",
    val fitnessLevel: String = "Intermediate",
    val activityLevel: String = "Moderate",
    val equipmentAccess: String = "Full Gym",
    val sessionMinutes: Int = 45,
    val daysPerWeek: Int = 4,
    val dietType: String = "balanced",
    val allergies: String = "",
    val medicalFlags: String = "",
    val disclaimerAcknowledged: Boolean = false,
    val bmr: Double = 0.0,
    val tdee: Double = 0.0,
    val calorieTarget: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false,
    val isLoading: Boolean = true,
    val hasProfile: Boolean = false,
    val isEditing: Boolean = false,
    val mode: ProfileMode = ProfileMode.View
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KinetiqApplication).repository

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var existingProfileId: String? = null
    private var existingDietaryPrefId: String? = null
    private var existingTargetId: String? = null

    init {
        loadExistingProfile()
    }

    fun loadExistingProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profile = repository.getProfile()
            val dietaryPreference = repository.getDietaryPreference()
            val target = repository.getUserTarget()

            if (profile != null) {
                existingProfileId = profile.id
                if (target != null) {
                    existingTargetId = target.id
                }
                if (dietaryPreference != null) {
                    existingDietaryPrefId = dietaryPreference.id
                }

                _uiState.value = _uiState.value.copy(
                    age = profile.age,
                    gender = profile.gender,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    goal = profile.goal,
                    fitnessLevel = profile.fitnessLevel,
                    activityLevel = profile.activityLevel,
                    equipmentAccess = profile.equipmentAccess,
                    sessionMinutes = profile.sessionMinutes,
                    daysPerWeek = profile.daysPerWeek,
                    dietType = dietaryPreference?.dietType ?: "balanced",
                    allergies = dietaryPreference?.allergies ?: "",
                    medicalFlags = dietaryPreference?.medicalFlags ?: "",
                    disclaimerAcknowledged = dietaryPreference?.disclaimerAckAt != null,
                    hasProfile = true,
                    isLoading = false,
                    isEditing = false,
                    mode = ProfileMode.View
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    hasProfile = false,
                    isLoading = false,
                    isEditing = false,
                    mode = ProfileMode.Onboarding
                )
            }

            recalculateTargets()
            if (target != null) {
                _uiState.value = _uiState.value.copy(
                    bmr = target.bmr,
                    tdee = target.tdee,
                    calorieTarget = target.calorieTarget,
                    proteinG = target.proteinG,
                    carbsG = target.carbsG,
                    fatG = target.fatG
                )
            }
        }
    }

    fun setEditing(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isEditing = enabled,
            mode = if (enabled) ProfileMode.Edit else ProfileMode.View
        )
    }

    fun setProfileMode(mode: ProfileMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            isEditing = mode == ProfileMode.Edit
        )
    }

    fun markSaved() {
        _uiState.value = _uiState.value.copy(
            isSavedSuccess = false,
            mode = ProfileMode.View,
            isEditing = false
        )
    }

    fun updateMetrics(age: Int, gender: String, heightCm: Double, weightKg: Double) {
        _uiState.value = _uiState.value.copy(
            age = age,
            gender = gender,
            heightCm = heightCm,
            weightKg = weightKg,
            errorMessage = null
        )
        recalculateTargets()
    }

    fun updateFitnessConfig(
        goal: String,
        fitnessLevel: String,
        activityLevel: String,
        equipmentAccess: String,
        daysPerWeek: Int,
        sessionMinutes: Int
    ) {
        _uiState.value = _uiState.value.copy(
            goal = goal,
            fitnessLevel = fitnessLevel,
            activityLevel = activityLevel,
            equipmentAccess = equipmentAccess,
            daysPerWeek = daysPerWeek,
            sessionMinutes = sessionMinutes,
            errorMessage = null
        )
        recalculateTargets()
    }

    fun updateNutritionAndHealth(
        dietType: String,
        allergies: String,
        medicalFlags: String,
        disclaimerAcknowledged: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            dietType = dietType,
            allergies = allergies,
            medicalFlags = medicalFlags,
            disclaimerAcknowledged = disclaimerAcknowledged,
            errorMessage = null
        )
    }

    fun setStep(step: Int) {
        if (step > _uiState.value.currentStep) {
            if (!validateCurrentStep()) return
        }
        _uiState.value = _uiState.value.copy(currentStep = step.coerceIn(1, 4), errorMessage = null)
    }

    fun nextStep() {
        setStep(_uiState.value.currentStep + 1)
    }

    fun previousStep() {
        setStep(_uiState.value.currentStep - 1)
    }

    private fun validateCurrentStep(): Boolean {
        val state = _uiState.value
        when (state.currentStep) {
            1 -> {
                if (state.age !in 1..120) {
                    _uiState.value = state.copy(errorMessage = "Please enter a valid age between 1 and 120.")
                    return false
                }
                if (!state.heightCm.isFinite() || state.heightCm <= 0 || state.heightCm > 300) {
                    _uiState.value = state.copy(errorMessage = "Please enter a valid height in cm.")
                    return false
                }
                if (!state.weightKg.isFinite() || state.weightKg <= 0 || state.weightKg > 500) {
                    _uiState.value = state.copy(errorMessage = "Please enter a valid weight in kg.")
                    return false
                }
            }
            3 -> {
                if (state.allergies.length > 200) {
                    _uiState.value = state.copy(errorMessage = "Allergies and dietary restrictions must be 200 characters or fewer.")
                    return false
                }
                if (state.medicalFlags.length > 500) {
                    _uiState.value = state.copy(errorMessage = "Medical restrictions must be 500 characters or fewer.")
                    return false
                }
                if (!state.disclaimerAcknowledged) {
                    _uiState.value = state.copy(errorMessage = "You must acknowledge the medical disclaimer to continue.")
                    return false
                }
            }
        }
        return true
    }

    fun recalculateTargets() {
        val state = _uiState.value
        val age = state.age
        val height = state.heightCm
        val weight = state.weightKg
        val isMale = state.gender.equals("Male", ignoreCase = true)

        if (age <= 0 || height <= 0 || weight <= 0) return

        val bmr = if (isMale) {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }

        val activityMultiplier = when (state.activityLevel.lowercase()) {
            "sedentary" -> 1.2
            "lightly active", "light" -> 1.375
            "moderate", "moderately active" -> 1.55
            "very active" -> 1.725
            "extra active" -> 1.9
            else -> 1.55
        }
        val tdee = bmr * activityMultiplier

        val rawCalorieTarget = when (state.goal.lowercase()) {
            "weight loss" -> tdee - 500
            "muscle gain" -> tdee + 400
            "recomposition" -> tdee - 100
            else -> tdee
        }

        val minCalorieFloor = if (isMale) 1500 else 1200
        val finalCalorieTarget = Math.max(rawCalorieTarget.toInt(), minCalorieFloor)

        val proteinG = weight * 2.0
        val proteinCalories = proteinG * 4.0

        val fatCalories = finalCalorieTarget * 0.25
        val fatG = fatCalories / 9.0

        val remainingCalories = Math.max(0.0, finalCalorieTarget - proteinCalories - fatCalories)
        val carbsG = remainingCalories / 4.0

        _uiState.value = state.copy(
            bmr = bmr,
            tdee = tdee,
            calorieTarget = finalCalorieTarget,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG
        )
    }

    fun saveFullProfile() {
        val stateBeforeSave = _uiState.value
        if (stateBeforeSave.age !in 1..120 || !stateBeforeSave.heightCm.isFinite() || stateBeforeSave.heightCm <= 0 || stateBeforeSave.heightCm > 300 ||
            !stateBeforeSave.weightKg.isFinite() || stateBeforeSave.weightKg <= 0 || stateBeforeSave.weightKg > 500) {
            _uiState.value = stateBeforeSave.copy(errorMessage = "Review age, height, and weight in Step 1 before saving.")
            return
        }
        if (stateBeforeSave.allergies.length > 200 || stateBeforeSave.medicalFlags.length > 500) {
            _uiState.value = stateBeforeSave.copy(errorMessage = "Shorten the allergy or medical restriction text before saving.")
            return
        }
        if (!stateBeforeSave.disclaimerAcknowledged) {
            _uiState.value = stateBeforeSave.copy(errorMessage = "You must acknowledge the medical disclaimer before saving.")
            return
        }
        if (!validateCurrentStep()) return

        viewModelScope.launch {
            val state = _uiState.value
            val userId = "default_user_id"
            val now = System.currentTimeMillis()

            val profileEntity = UserProfileEntity(
                id = existingProfileId ?: java.util.UUID.randomUUID().toString(),
                userId = userId,
                age = state.age,
                gender = state.gender,
                heightCm = state.heightCm,
                weightKg = state.weightKg,
                fitnessLevel = state.fitnessLevel,
                activityLevel = state.activityLevel,
                goal = state.goal,
                equipmentAccess = state.equipmentAccess,
                sessionMinutes = state.sessionMinutes,
                daysPerWeek = state.daysPerWeek,
                updatedAt = now
            )

            val dietaryPrefEntity = DietaryPreferenceEntity(
                id = existingDietaryPrefId ?: java.util.UUID.randomUUID().toString(),
                userId = userId,
                dietType = state.dietType,
                allergies = state.allergies.ifBlank { null },
                medicalFlags = state.medicalFlags.ifBlank { null },
                disclaimerAckAt = if (state.disclaimerAcknowledged) now else null,
                updatedAt = now
            )

            val targetEntity = UserTargetEntity(
                id = existingTargetId ?: java.util.UUID.randomUUID().toString(),
                userId = userId,
                bmr = state.bmr,
                tdee = state.tdee,
                calorieTarget = state.calorieTarget,
                proteinG = state.proteinG,
                carbsG = state.carbsG,
                fatG = state.fatG,
                effectiveDate = now,
                updatedAt = now
            )

            try {
                repository.saveFullProfileAndTargets(profileEntity, dietaryPrefEntity, targetEntity)
                existingProfileId = profileEntity.id
                existingDietaryPrefId = dietaryPrefEntity.id
                existingTargetId = targetEntity.id
                _uiState.value = state.copy(isSavedSuccess = true, errorMessage = null, mode = ProfileMode.View, isEditing = false)
            } catch (e: Exception) {
                _uiState.value = state.copy(errorMessage = "Failed to save profile: ${e.message}")
            }
        }
    }
}
