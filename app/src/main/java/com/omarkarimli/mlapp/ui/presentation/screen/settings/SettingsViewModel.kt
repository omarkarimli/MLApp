package com.omarkarimli.mlapp.ui.presentation.screen.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.repository.SharedPreferenceRepository
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import com.omarkarimli.mlapp.ui.theme.AppTheme
import com.omarkarimli.mlapp.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sharedPreferenceRepository: SharedPreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isNotificationsEnabled = MutableStateFlow(false)
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Load notifications setting with a default value of 'true'
                _isNotificationsEnabled.value = sharedPreferenceRepository.getBoolean(
                    Constants.NOTIFICATION_KEY,
                    true
                )
                _uiState.value = UiState.Success("Settings loaded successfully.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading settings", e)
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun onNotificationsToggle(isEnabled: Boolean) {
        viewModelScope.launch {
            _isNotificationsEnabled.value = isEnabled
            sharedPreferenceRepository.saveBoolean(Constants.NOTIFICATION_KEY, isEnabled)
            _uiState.value = UiState.Success("Notifications setting updated.")
        }
    }

    fun clearSharedPreferences(currentTheme: AppTheme) {
        viewModelScope.launch {
            sharedPreferenceRepository.clearSharedPreferences()

            // After clearing, re-save the essential preferences
            // This prevents the app from losing its state after a reset
            sharedPreferenceRepository.saveBoolean(Constants.LOGIN_KEY, true)
            sharedPreferenceRepository.saveString(Constants.THEME_KEY, currentTheme.name.lowercase())
            sharedPreferenceRepository.saveBoolean(Constants.NOTIFICATION_KEY, true) // Reset notifications to default

            // Re-load settings to reflect the default values in the UI
            loadSettings()
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}