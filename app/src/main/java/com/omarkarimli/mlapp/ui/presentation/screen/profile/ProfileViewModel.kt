package com.omarkarimli.mlapp.ui.presentation.screen.profile

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
class ProfileViewModel @Inject constructor(
    private val sharedPreferenceRepository: SharedPreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _bioText = MutableStateFlow("")
    val bioText: StateFlow<String> = _bioText.asStateFlow()

    private val _websiteText = MutableStateFlow("")
    val websiteText: StateFlow<String> = _websiteText.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                _bioText.value = sharedPreferenceRepository.getString(Constants.BIO, "")
                _websiteText.value = sharedPreferenceRepository.getString(Constants.WEBSITE, "")
                _uiState.value = UiState.Success("Settings loaded successfully.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading settings", e)
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun saveSettings(bioText: String, websiteText: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                sharedPreferenceRepository.saveString(Constants.BIO, bioText)
                sharedPreferenceRepository.saveString(Constants.WEBSITE, websiteText)
                _uiState.value = UiState.Success("Settings saved successfully.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error saving settings", e)
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}