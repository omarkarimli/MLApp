package com.omarkarimli.mlapp.ui.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(scrollBehavior)
        }
    ) { innerPadding ->
        ScrollContent(innerPadding, navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
    val isTopAppBarMinimized = scrollBehavior.state.collapsedFraction > 0.5 // Adjust threshold as needed
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                "Settings",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isTopAppBarMinimized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            FilledIconButton(
                onClick = { /* do something */ },
                modifier = Modifier.size(Dimens.IconSizeLarge), // Changed from 32.dp
                shape = IconButtonDefaults.filledShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.QuestionMark,
                    contentDescription = "Help",
                    modifier = Modifier.size(Dimens.IconSizeSmall),
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall)) // Changed from 8.dp
            FilledTonalIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier
                    .width(Dimens.IconSizeExtraLarge) // Changed from 48.dp
                    .height(Dimens.IconSizeLarge), // Changed from 32.dp
                shape = IconButtonDefaults.filledShape
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(Dimens.IconSizeSmall),
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall))
        }
    )
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues,
    navController: NavHostController
) {
    // Sample data for your list
    val items = listOf(
        ListItemModel(
            "1",
            Icons.Rounded.Notifications,
            "Notifications",
            "Manage your alerts and sounds.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "2",
            Icons.Rounded.DarkMode, // Common icon for theme settings
            "Theme",
            "Change the app's appearance.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "3",
            Icons.Rounded.Language, // Standard icon for language settings
            "Language",
            "Select your preferred language.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "4",
            Icons.Rounded.SettingsBackupRestore, // Icon suggesting a reset
            "Reset Settings",
            "Revert all settings to their default values.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "5",
            Icons.Outlined.Info,
            "About App",
            "View app information and version.",
            Icons.AutoMirrored.Rounded.ArrowForward
        )
    )

    Column(
        modifier = Modifier
            .padding(
                top = innerPadding.calculateTopPadding() - Dimens.PaddingLarge,
                bottom = innerPadding.calculateBottomPadding()
            ) // Apply innerPadding to the whole column
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(Dimens.SpacerLarge))

        // LazyColumn for the scrollable list - now it takes the remaining space
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall) // Space between list items
        ) {
            items(items) { item ->
                ListItemUi(item = item)
            }
        }
    }
}