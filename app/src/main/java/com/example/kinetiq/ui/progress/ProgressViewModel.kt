package com.example.kinetiq.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetiq.KinetiqApplication
import com.example.kinetiq.data.local.entity.GpsLogEntity
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import com.example.kinetiq.data.local.entity.WeightLogEntity
import com.example.kinetiq.utils.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ProgressUiState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val capturedPhotoPath: String? = null,
    val weightLogs: List<WeightLogEntity> = emptyList(),
    val progressPhotos: List<ProgressPhotoEntity> = emptyList()
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as KinetiqApplication).repository
    private val userId = "default_user_id"
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllWeightLogsFlow(userId).collectLatest { logs ->
                _uiState.value = _uiState.value.copy(weightLogs = logs.take(20))
            }
        }
        viewModelScope.launch {
            repository.getAllProgressPhotosFlow(userId).collectLatest { photos ->
                _uiState.value = _uiState.value.copy(progressPhotos = photos.take(30))
            }
        }
    }

    fun createPhotoFile(): File {
        val directory = File(getApplication<Application>().filesDir, "images/progress")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Could not create the progress photo folder.")
        }
        return File(directory, "progress_${UUID.randomUUID()}.jpg")
    }

    fun setCapturedPhoto(path: String) {
        _uiState.value = _uiState.value.copy(capturedPhotoPath = path, message = null)
    }

    fun clearCapturedPhoto() {
        _uiState.value = _uiState.value.copy(capturedPhotoPath = null)
    }

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun saveWeight(weightText: String) {
        if (_uiState.value.isSaving) return
        val weight = weightText.trim().toDoubleOrNull()
        if (weight == null || !weight.isFinite() || weight <= 0.0 || weight > 500.0) {
            showMessage("Enter a valid weight in kg.")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val calendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = now
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.timeInMillis
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.timeInMillis - 1

                val gps = LocationUtils.getCurrentLocation(getApplication())
                repository.saveGpsLog(gps)

                val existingTodayLog = repository.getTodayWeightLog(userId, startOfDay, endOfDay)
                if (existingTodayLog != null) {
                    val updatedLog = existingTodayLog.copy(
                        weightKg = weight,
                        gpsLogId = gps.id,
                        loggedAt = now
                    )
                    repository.updateWeightLog(updatedLog)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = "Today's weight updated to ${weight} kg."
                    )
                } else {
                    repository.saveWeightLog(WeightLogEntity(userId = userId, weightKg = weight, gpsLogId = gps.id, loggedAt = now))
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = "Weight saved (${weight} kg)."
                    )
                }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, message = error.localizedMessage ?: "Could not save weight.")
            }
        }
    }

    fun saveProgressPhoto(angle: String) {
        if (_uiState.value.isSaving) return
        val path = _uiState.value.capturedPhotoPath
        if (path.isNullOrBlank() || !File(path).isFile) {
            showMessage("Capture a progress photo first.")
            return
        }
        if (angle !in setOf("Front", "Side", "Back")) {
            showMessage("Choose a photo angle.")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        viewModelScope.launch {
            try {
                val gps: GpsLogEntity = LocationUtils.getCurrentLocation(getApplication())
                repository.saveGpsLog(gps)
                repository.saveProgressPhoto(
                    ProgressPhotoEntity(userId = userId, photoUrl = path, angle = angle, gpsLogId = gps.id)
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = if (gps.gpsOk) "Progress photo saved with location." else "Progress photo saved. Location unavailable.",
                    capturedPhotoPath = null
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, message = error.localizedMessage ?: "Could not save progress photo.")
            }
        }
    }
}
