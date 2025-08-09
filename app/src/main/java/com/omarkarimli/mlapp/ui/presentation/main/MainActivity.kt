package com.omarkarimli.mlapp.ui.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.omarkarimli.mlapp.ui.navigation.AppNavigation
import com.omarkarimli.mlapp.ui.theme.AppTheme
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val currentTheme by mainViewModel.currentTheme.collectAsState()
            val isDynamicColorEnabled by mainViewModel.isDynamicColorEnabled.collectAsState()

            MLAppTheme(
                dynamicColor = isDynamicColorEnabled,
                darkTheme = when (currentTheme) {
                    AppTheme.Dark -> true
                    AppTheme.Light -> false
                    AppTheme.System -> isSystemInDarkTheme()
                }
            ) {
                AppNavigation(mainViewModel)
            }
        }
    }
}