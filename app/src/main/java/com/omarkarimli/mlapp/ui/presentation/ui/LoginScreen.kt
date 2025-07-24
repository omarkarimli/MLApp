package com.omarkarimli.mlapp.ui.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Import this
import androidx.compose.runtime.mutableStateOf // Import this
import androidx.compose.runtime.remember // Import this
import androidx.compose.runtime.setValue // Import this
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun LoginScreen(navController: NavHostController) {
    // State for the Bio TextField
    var bioText by remember { mutableStateOf("") }
    // State for the Website TextField
    var websiteText by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.PaddingMedium),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "App Icon",
                modifier = Modifier.size(Dimens.IconSizeExtraLarge + Dimens.IconSizeExtraLarge), // 96.dp
            )
            Spacer(modifier = Modifier.height(Dimens.SpacerHeightLarge)) // 48.dp
            Text(
                "Bio",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Dimens.PaddingSmall)
            )
            TextField(
                value = bioText, // Use the state variable here
                onValueChange = { newText -> bioText = newText }, // Update the state
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Optional") },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.height(Dimens.SpacerMedium)) // 20.dp
            Text(
                "Website",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Dimens.PaddingSmall)
            )
            TextField(
                value = websiteText, // Use the state variable here
                onValueChange = { newText -> websiteText = newText }, // Update the state
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Optional") },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.height(Dimens.SpacerLarge)) // 32.dp
            Button(
                onClick = {
                    onLoginClick(navController)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeight), // Use Dimens for button height
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge), // Use Dimens for corner radius
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Continue via Google",
                    letterSpacing = Dimens.LetterSpacingButton, // Use Dimens for letter spacing
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun onLoginClick(navController: NavHostController) {
    navController.navigate(Screen.Home.route) {
        popUpTo(Screen.Login.route) { inclusive = true }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    MLAppTheme {
        LoginScreen(navController = NavHostController(LocalContext.current))
    }
}