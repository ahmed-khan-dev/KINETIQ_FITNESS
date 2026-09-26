package com.example.kinetiq.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _welcomeMessage = MutableStateFlow("Welcome to Kinetiq Home")
    val welcomeMessage: StateFlow<String> = _welcomeMessage.asStateFlow()
}