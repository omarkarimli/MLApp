package com.omarkarimli.mlapp.ui.presentation.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.repository.SharedPreferenceRepository
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import com.omarkarimli.mlapp.utils.Constants
import com.omarkarimli.mlapp.utils.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val sharedPreferenceRepository: SharedPreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(Dimens.splashDuration)

            val loginKey = sharedPreferenceRepository.getBoolean(Constants.LOGIN_KEY, false)
            _uiState.value = if (loginKey) UiState.Success("Login successful") else UiState.Error("Login failed")
        }
    }
}