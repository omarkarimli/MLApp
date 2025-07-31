package com.omarkarimli.mlapp.ui.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omarkarimli.mlapp.ui.navigation.AppNavigation
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val isDarkMode by mainViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()

            MLAppTheme(
                darkTheme = isDarkMode
            ) {
                AppNavigation(mainViewModel)
            }
        }
    }
}