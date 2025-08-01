package com.omarkarimli.mlapp.ui.presentation.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omarkarimli.mlapp.domain.models.StandardListItemModel
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.widget.StandardListItemUi
import com.omarkarimli.mlapp.ui.presentation.main.MainViewModel
import com.omarkarimli.mlapp.utils.Constants
import com.omarkarimli.mlapp.utils.Dimens
import com.omarkarimli.mlapp.utils.getVersionNumber
import com.omarkarimli.mlapp.utils.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(mainViewModel: MainViewModel) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val isDarkModeEnabled by mainViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(scrollBehavior, context)
        }
    ) { innerPadding ->
        ScrollContent(
            context = context,
            innerPadding = innerPadding,
            isNotificationsEnabled = isNotificationsEnabled,
            isDarkModeEnabled = isDarkModeEnabled,
            onNotificationsToggle = viewModel::onNotificationsToggle,
            onDarkModeToggle = mainViewModel::onThemeChange,
            // Pass the new function to the content
            onClearPreferences = { viewModel.clearSharedPreferences(isDarkModeEnabled) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(scrollBehavior: TopAppBarScrollBehavior, context: Context) {
    val navController = LocalNavController.current

    val isTopAppBarMinimized = scrollBehavior.state.collapsedFraction > 0.5
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                Screen.Settings.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isTopAppBarMinimized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            FilledIconButton(
                onClick = { context.openUrl(Constants.HELP_URL) },
                modifier = Modifier.size(Dimens.IconSizeLarge),
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
            Spacer(Modifier.size(Dimens.PaddingSmall))
            FilledTonalIconButton(
                onClick = { navController.navigate(Screen.Profile.route) },
                modifier = Modifier
                    .width(Dimens.IconSizeExtraLarge)
                    .height(Dimens.IconSizeLarge),
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
    context: Context,
    innerPadding: PaddingValues,
    isNotificationsEnabled: Boolean,
    isDarkModeEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    // Add the new function parameter here
    onClearPreferences: () -> Unit
) {
    var dialogState by remember { mutableStateOf<String?>(null) }

    val items = listOf(
        StandardListItemModel(
            "1",
            Icons.Rounded.Notifications,
            "Notifications",
            "Manage your alerts and sounds.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { dialogState = "notifications" }
        ),
        StandardListItemModel(
            "2",
            Icons.Rounded.DarkMode,
            "Dark mode",
            "Change the app's appearance.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { dialogState = "darkmode" }
        ),
        StandardListItemModel(
            "3",
            Icons.Rounded.SettingsBackupRestore,
            "Reset Settings",
            "Revert all settings to their default values.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            // Update the onClick lambda to set the dialogState
            onClick = { dialogState = "resetSettings" }
        ),
        StandardListItemModel(
            "4",
            Icons.Outlined.Info,
            "About App",
            "View app information and version.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            // Update the onClick lambda to set the dialogState
            onClick = { dialogState = "aboutApp" }
        )
    )

    Column(
        modifier = Modifier.padding(innerPadding).fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(Dimens.SpacerLarge))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
            contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge)
        ) {
            items(items) { item ->
                StandardListItemUi(item = item)
            }
        }
    }

    when (dialogState) {
        "notifications" -> {
            SettingsSelectionDialog(
                title = "Notifications",
                options = listOf("On", "Off"),
                selectedOption = if (isNotificationsEnabled) "On" else "Off",
                onOptionSelected = {
                    val isEnabled = it == "On"
                    onNotificationsToggle(isEnabled)
                    dialogState = null
                },
                onDismiss = { dialogState = null }
            )
        }
        "darkmode" -> {
            SettingsSelectionDialog(
                title = "Dark mode",
                options = listOf("Light", "Dark"),
                selectedOption = if (isDarkModeEnabled) "Dark" else "Light",
                onOptionSelected = {
                    val isEnabled = it == "Dark"
                    onDarkModeToggle(isEnabled)
                    dialogState = null
                },
                onDismiss = { dialogState = null }
            )
        }
        // New AlertDialog for Reset Settings
        "resetSettings" -> {
            AlertDialog(
                onDismissRequest = { dialogState = null },
                title = { Text(text = "Reset Settings") },
                text = { Text(text = "Are you sure you want to revert all settings to their default values? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onClearPreferences() // Call the function to clear preferences
                            dialogState = null
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogState = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        // New AlertDialog for About App
        "aboutApp" -> {
            AlertDialog(
                onDismissRequest = { dialogState = null },
                title = { Text(text = "About App") },
                text = {
                    Text(
                        text = "Version: " + context.getVersionNumber() + "\n\n" +
                                "This is a machine learning companion app with Google ML Kit.\n" +
                                "For support, please contact: " + Constants.MY_EMAIL
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            context.openUrl(Constants.MY_LINKEDIN)
                            dialogState = null
                        }
                    ) {
                        Text("Open LinkedIn")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            context.openUrl("mailto:${Constants.MY_EMAIL}")
                            dialogState = null
                        }
                    ) {
                        Text("Send Email")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (tempSelected, onOptionClick) = remember { mutableStateOf(selectedOption) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = (option == tempSelected),
                                onClick = { onOptionClick(option) },
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == tempSelected),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOptionSelected(tempSelected)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
