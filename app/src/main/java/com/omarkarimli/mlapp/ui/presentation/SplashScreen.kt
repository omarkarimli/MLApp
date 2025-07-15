package com.omarkarimli.mlapp.ui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import androidx.navigation.compose.rememberNavController
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun SplashScreen(navController: NavHostController) {
    Scaffold { innerPadding ->
        MainContent(innerPadding, navController)
    }
}

@Preview(showBackground = true)
@Composable
fun SplashPreview() {
    MLAppTheme {
        SplashScreen(navController = rememberNavController()) // Use rememberNavController for previews
    }
}

@Composable
private fun MainContent(
    innerPadding: PaddingValues,
    navController: NavHostController
) {
    var animateText by remember { mutableStateOf(false) }

    // Trigger the animation after a short delay
    LaunchedEffect(Unit) {
        delay(300) // Small delay to show the icon alone for a moment
        animateText = true

        // Calculate total animation duration
        // Icon delay (300ms) + text slide-in duration (700ms)
        val totalAnimationDuration = 300L + 700L

        delay(totalAnimationDuration) // Wait for the animation to complete

        // Navigate to the Home screen
        // Use popUpTo to prevent the user from coming back to the splash screen
        navController.navigate(Screen.Onboarding.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = "App Icon",
                modifier = Modifier.size(96.dp)
            )
            AnimatedVisibility(
                visible = animateText,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth }, // Starts from right outside the screen
                    animationSpec = tween(durationMillis = 700)
                ) + fadeIn(animationSpec = tween(durationMillis = 700))
            ) {
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.app_name),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}