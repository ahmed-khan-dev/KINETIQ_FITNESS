package com.example.kinetiq.ui.meal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetiq.KinetiqApplication
import com.example.kinetiq.data.local.entity.GpsLogEntity
import com.example.kinetiq.data.local.entity.MealLogEntity
import com.example.kinetiq.utils.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class MealForm(
    val name: String,
    val slot: String,
    val calories: String,
    val proteinG: String,
    val carbsG: String,
    val fatG: String
)

data class MealUiState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val capturedPhotoPath: String? = null,
    val recentMeals: List<MealLogEntity> = emptyList()
)

class MealViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as KinetiqApplication).repository
    private val userId = "default_user_id"

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllMealsFlow(userId).collectLatest { meals ->
                _uiState.value = _uiState.value.copy(recentMeals = meals.take(10))
            }
        }
    }

    fun createPhotoFile(): File {
        val directory = File(getApplication<Application>().filesDir, "images/meals")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Could not create the meal photo folder.")
        }
        return File(directory, "meal_${System.currentTimeMillis()}.jpg")
    }

    fun setCapturedPhoto(path: String) {
        _uiState.value = _uiState.value.copy(capturedPhotoPath = path, message = null)
    }

    fun clearCapturedPhoto() {
        _uiState.value = _uiState.value.copy(capturedPhotoPath = null)
    }

    fun saveMeal(form: MealForm) {
        val current = _uiState.value
        if (current.isSaving) return
        val name = form.name.trim()
        val photoPath = current.capturedPhotoPath
        val calories = form.calories.toIntOrNull()
        val protein = if (form.proteinG.isBlank()) 0.0 else form.proteinG.trim().toDoubleOrNull()
        val carbs = if (form.carbsG.isBlank()) 0.0 else form.carbsG.trim().toDoubleOrNull()
        val fat = if (form.fatG.isBlank()) 0.0 else form.fatG.trim().toDoubleOrNull()

        if (name.isBlank() || name.length > 100) return showMessage("Enter a meal name of 1 to 100 characters.")
        if (photoPath.isNullOrBlank() || !File(photoPath).isFile) return showMessage("Capture a meal photo first.")
        if (calories == null || calories !in 0..10000) return showMessage("Enter calories from 0 to 10,000.")
        if (listOf(protein, carbs, fat).any { it == null || !it.isFinite() || it < 0.0 || it > 1000.0 }) {
            return showMessage("Enter nutrition values from 0 to 1,000 g, or leave them blank.")
        }

        _uiState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch {
            try {
                val gpsLog: GpsLogEntity = LocationUtils.getCurrentLocation(getApplication())
                repository.saveGpsLog(gpsLog)
                val now = System.currentTimeMillis()
                repository.saveMealLog(
                    MealLogEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        photoUrl = photoPath,
                        loggedAt = now,
                        mealSlot = form.slot,
                        mealName = name,
                        calories = calories,
                        proteinG = protein ?: 0.0,
                        carbsG = carbs ?: 0.0,
                        fatG = fat ?: 0.0,
                        gpsLogId = gpsLog.id,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = if (gpsLog.gpsOk) "Meal saved with location." else "Meal saved. Location unavailable."
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = error.localizedMessage ?: "Could not save this meal."
                )
            }
        }
    }

    fun deleteMeal(meal: MealLogEntity) {
        viewModelScope.launch {
            try {
                repository.deleteMealLog(meal)
                showMessage("Meal '${meal.mealName}' deleted.")
            } catch (error: Exception) {
                showMessage("Could not delete meal: ${error.localizedMessage}")
            }
        }
    }

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }
}
