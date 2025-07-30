package com.omarkarimli.mlapp.ui.presentation.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.omarkarimli.mlapp.utils.Dimens

@HiltViewModel
class SplashViewModel @Inject constructor(
    val imageLoader: ImageLoader // Hilt will inject the ImageLoader provided by AppModule
) : ViewModel() {

    private val _navigateToOnboarding = MutableStateFlow(false)
    val navigateToOnboarding: StateFlow<Boolean> = _navigateToOnboarding

    init {
        viewModelScope.launch {
            delay(Dimens.splashDuration)
            _navigateToOnboarding.value = true
        }
    }
}