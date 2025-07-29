package com.omarkarimli.mlapp.ui.presentation.ui.splash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.utils.Dimens

import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SplashScreen(navController: NavHostController) {
    val viewModel: SplashViewModel = hiltViewModel()

    val context = LocalContext.current
    val navigate by viewModel.navigateToOnboarding.collectAsState()

    // The imageLoader is now provided by the ViewModel (which Hilt injected)
    val imageLoader = viewModel.imageLoader

    val splashImage = ImageRequest.Builder(context)
        .data(R.raw.splash)
        .crossfade(true)
        .build()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color.White
        ) {
            AsyncImage(
                model = splashImage,
                imageLoader = imageLoader, // Use the Hilt-provided imageLoader
                contentDescription = "Splash GIF",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.PaddingExtraLarge),
                contentScale = ContentScale.Fit
            )

            LaunchedEffect(key1 = navigate) {
                if (navigate) {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }
    }
}