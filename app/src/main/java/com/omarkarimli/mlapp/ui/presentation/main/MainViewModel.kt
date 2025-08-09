package com.omarkarimli.mlapp.ui.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.repository.SharedPreferenceRepository
import com.omarkarimli.mlapp.domain.repository.ThemeRepository
import com.omarkarimli.mlapp.ui.theme.AppTheme
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
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(AppTheme.System)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _isDynamicColorEnabled = MutableStateFlow(false)
    val isDynamicColorEnabled: StateFlow<Boolean> = _isDynamicColorEnabled.asStateFlow()

    init {
        loadInitialTheme()
    }

    fun loadInitialTheme() {
        viewModelScope.launch {
            // Load theme setting
            val savedThemeName = sharedPreferenceRepository.getString(Constants.THEME_KEY, "system")
            val initialTheme = when (savedThemeName) {
                "light" -> AppTheme.Light
                "dark" -> AppTheme.Dark
                else -> AppTheme.System
            }
            _currentTheme.value = initialTheme

            // Load dynamic color setting
            val isDynamicColorEnabled = sharedPreferenceRepository.getBoolean(Constants.DYNAMIC_COLOR_KEY, false)
            _isDynamicColorEnabled.value = isDynamicColorEnabled
        }
    }

    fun onThemeChange(newTheme: AppTheme) {
        viewModelScope.launch {
            _currentTheme.value = newTheme
            sharedPreferenceRepository.saveString(Constants.THEME_KEY, newTheme.name.lowercase())
            themeRepository.applyTheme(newTheme)
        }
    }

    fun onDynamicColorToggle(isEnabled: Boolean) {
        viewModelScope.launch {
            _isDynamicColorEnabled.value = isEnabled
            sharedPreferenceRepository.saveBoolean(Constants.DYNAMIC_COLOR_KEY, isEnabled)
        }
    }
}