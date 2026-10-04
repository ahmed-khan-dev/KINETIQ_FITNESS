package com.example.kinetiq.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetiq.KinetiqApplication
import com.example.kinetiq.data.local.entity.AppSessionEntity
import com.example.kinetiq.utils.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LockState {
    object Idle : LockState()
    object CheckingSession : LockState()
    object AutoBypassed : LockState()
    data class PinRequired(val hasPin: Boolean) : LockState()
    object Authenticated : LockState()
    data class Error(val message: String) : LockState()
}

class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KinetiqApplication).repository

    private val _lockState = MutableStateFlow<LockState>(LockState.Idle)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private var currentSession: AppSessionEntity? = null

    fun checkSession() {
        viewModelScope.launch {
            _lockState.value = LockState.CheckingSession
            try {
                val session = repository.getAppSession()
                currentSession = session

                if (session != null && session.isAuthenticated) {
                    val now = System.currentTimeMillis()
                    if (now < session.sessionExpiryTimestamp) {
                        repository.updateSessionSuccess()
                        _lockState.value = LockState.AutoBypassed
                        return@launch
                    }
                }

                val hasPin = !session?.pinHash.isNullOrEmpty()
                _lockState.value = LockState.PinRequired(hasPin = hasPin)
            } catch (e: Exception) {
                _lockState.value = LockState.PinRequired(hasPin = false)
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            repository.updateSessionSuccess()
            _lockState.value = LockState.Authenticated
        }
    }

    fun submitPin(pin: String) {
        viewModelScope.launch {
            if (pin.length != 4 || pin.any { it !in '0'..'9' }) {
                _lockState.value = LockState.Error("PIN must be 4 digits")
                return@launch
            }

            val session = repository.getAppSession()
            val storedHash = session?.pinHash

            if (storedHash.isNullOrEmpty()) {
                repository.setPinHash(pin)
                repository.updateSessionSuccess()
                _lockState.value = LockState.Authenticated
            } else {
                if (SecurityUtils.verifyPin(pin, storedHash)) {
                    repository.updateSessionSuccess()
                    _lockState.value = LockState.Authenticated
                } else {
                    _lockState.value = LockState.Error("Incorrect PIN. Please try again.")
                }
            }
        }
    }
}
