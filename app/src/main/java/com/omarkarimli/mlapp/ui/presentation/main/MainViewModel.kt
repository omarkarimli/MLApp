package com.omarkarimli.mlapp.ui.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.repository.SharedPreferenceRepository
import com.omarkarimli.mlapp.domain.repository.ThemeRepository
import com.omarkarimli.mlapp.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sharedPreferenceRepository: SharedPreferenceRepository,
    val themeRepository: ThemeRepository
) : ViewModel() {

    private val _isDarkModeEnabled = MutableStateFlow(false)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()

    init {
        loadInitialTheme()
    }

    private fun loadInitialTheme() {
        viewModelScope.launch {
            _isDarkModeEnabled.value = sharedPreferenceRepository.getBoolean(
                Constants.DARK_MODE,
                false
            )
            // Apply the theme on startup
            themeRepository.applyTheme(_isDarkModeEnabled.value)
        }
    }

    fun onThemeChange(isDarkMode: Boolean) {
        viewModelScope.launch {
            _isDarkModeEnabled.value = isDarkMode
            sharedPreferenceRepository.saveBoolean(Constants.DARK_MODE, isDarkMode)

            themeRepository.applyTheme(isDarkMode)
        }
    }
}