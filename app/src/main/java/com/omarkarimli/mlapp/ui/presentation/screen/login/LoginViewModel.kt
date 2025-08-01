package com.omarkarimli.mlapp.ui.presentation.screen.login

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
class LoginViewModel @Inject constructor(
    val sharedPreferenceRepository: SharedPreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onContinue(bioText: String, websiteText: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            sharedPreferenceRepository.saveString(Constants.BIO, bioText)
            sharedPreferenceRepository.saveString(Constants.WEBSITE, websiteText)
            sharedPreferenceRepository.saveBoolean(Constants.LOGIN_KEY, true)
            sharedPreferenceRepository.saveBoolean(Constants.NOTIFICATION_KEY, false)

            _uiState.value = UiState.Success("Logged in successfully")
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}