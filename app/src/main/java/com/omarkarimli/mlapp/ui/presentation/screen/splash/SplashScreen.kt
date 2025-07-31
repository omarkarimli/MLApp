package com.omarkarimli.mlapp.ui.presentation.screen.splash

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import com.omarkarimli.mlapp.ui.presentation.common.widget.WeightedImageDisplay
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun SplashScreen() {
    val viewModel: SplashViewModel = hiltViewModel()
    val navController = LocalNavController.current

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            UiState.Loading -> {}
            is UiState.Success -> {
                val successMessage = (uiState as UiState.Success).message
                Log.e("SplashScreen", "Success: $successMessage")
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is UiState.Error -> {
                val errorMessage = (uiState as UiState.Error).message
                Log.e("SplashScreen", "Error: $errorMessage")
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is UiState.PermissionAction -> {}
            UiState.Idle -> {}
        }
    }

    Scaffold { innerPadding ->
        WeightedImageDisplay(
            modifier = Modifier.padding(innerPadding).padding(
                start = Dimens.PaddingMedium,
                end = Dimens.PaddingMedium,
                top = Dimens.PaddingMedium
            )
        )
    }
}
