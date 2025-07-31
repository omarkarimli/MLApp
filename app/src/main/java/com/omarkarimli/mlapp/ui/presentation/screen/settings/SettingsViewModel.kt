package com.omarkarimli.mlapp.ui.presentation.screen.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.repository.SharedPreferenceRepository
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
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
                _isNotificationsEnabled.value = sharedPreferenceRepository.getBoolean(
                    Constants.NOTIFICATION_KEY,
                    true // Default value is often 'on' for notifications
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
            // Optional: Provide UI feedback
            _uiState.value = UiState.Success("Notifications setting updated.")
        }
    }

    fun clearSharedPreferences(isDarkModeEnabled: Boolean) {
        viewModelScope.launch {
            sharedPreferenceRepository.clearSharedPreferences()
            sharedPreferenceRepository.saveBoolean(Constants.LOGIN_KEY, true)
            sharedPreferenceRepository.saveBoolean(Constants.DARK_MODE, isDarkModeEnabled)
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}