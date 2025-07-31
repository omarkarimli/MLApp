package com.omarkarimli.mlapp.ui.presentation.screen.login

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import com.omarkarimli.mlapp.ui.presentation.common.widget.MyTextField
import com.omarkarimli.mlapp.ui.presentation.common.widget.WeightedImageDisplay
import com.omarkarimli.mlapp.ui.presentation.screen.login.LoginViewModel
import com.omarkarimli.mlapp.utils.Dimens
import com.omarkarimli.mlapp.utils.showToast

@Composable
fun LoginScreen() {
    val viewModel: LoginViewModel = hiltViewModel()
    val navController = LocalNavController.current
    val context = LocalContext.current

    var bioText by remember { mutableStateOf("") }
    var websiteText by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            UiState.Loading -> { /* Handle loading if needed */ }
            is UiState.Success -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }

                context.showToast("Login successful")
            }
            is UiState.Error -> {
                val errorMessage = (uiState as UiState.Error).message
                context.showToast(errorMessage)
                Log.e("LoginScreen", "Error: $errorMessage")

                viewModel.resetUiState()
            }
            is UiState.PermissionAction -> { /* Nothing */ }
            UiState.Idle -> { /* Hide any loading indicators */ }
        }
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.PaddingMedium),
            horizontalAlignment = Alignment.Start,
        ) {
            WeightedImageDisplay(
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(Dimens.SpacerHeightLarge))
            MyTextField("Bio", "Optional", bioText) {
                bioText = it
            }
            Spacer(modifier = Modifier.height(Dimens.SpacerMedium))
            MyTextField("Website", "Optional", websiteText) {
                websiteText = it
            }
            Spacer(modifier = Modifier.height(Dimens.SpacerLarge))
            Button(
                onClick = { viewModel.onContinue(bioText, websiteText, isSystemInDarkTheme) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeight),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Continue",
                    letterSpacing = Dimens.LetterSpacingButton,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
