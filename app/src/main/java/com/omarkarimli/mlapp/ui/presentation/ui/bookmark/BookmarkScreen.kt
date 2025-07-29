@file:OptIn(ExperimentalMaterial3Api::class)

package com.omarkarimli.mlapp.ui.presentation.ui.bookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.ui.presentation.ui.common.widget.ResultCard
import com.omarkarimli.mlapp.utils.Dimens
import androidx.hilt.navigation.compose.hiltViewModel
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.theme.MLAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val viewModel: BookmarkViewModel = hiltViewModel()
    val savedResults by viewModel.savedResults.collectAsState()

    // State to control the visibility of the alert dialog
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(
                scrollBehavior = scrollBehavior,
                onClearAllClicked = { showClearAllDialog = true } // Set dialog to visible
            )
        }
    ) { innerPadding ->
        ScrollContent(innerPadding, savedResults)
    }

    // Show the AlertDialog if showClearAllDialog is true
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false }, // Dismiss the dialog if clicked outside
            title = { Text("Clear All Bookmarks?") },
            text = { Text("Are you sure you want to delete all your saved bookmarks? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllSavedResults() // Perform the clear operation
                        showClearAllDialog = false // Dismiss the dialog
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false } // Dismiss the dialog
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onClearAllClicked: () -> Unit) {
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
                Screen.Bookmarks.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isTopAppBarMinimized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        actions = {
            FilledTonalIconButton(
                onClick = onClearAllClicked, // Call the lambda when clicked
                modifier = Modifier
                    .width(Dimens.IconSizeExtraLarge)
                    .height(Dimens.IconSizeLarge),
                shape = IconButtonDefaults.filledShape
            ) {
                Icon(
                    Icons.Rounded.ClearAll,
                    modifier = Modifier.size(Dimens.IconSizeSmall),
                    contentDescription = "Clear all bookmarks"
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall))
        }
    )
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues,
    savedResults: List<ResultCardModel>
) {
    var searchText by remember { mutableStateOf("") }

    val filteredSavedResults = remember(searchText, savedResults) {
        if (searchText.isBlank()) {
            savedResults
        } else {
            savedResults.filter {
                it.title.contains(searchText, ignoreCase = true) || it.subtitle.contains(searchText, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(
                top = innerPadding.calculateTopPadding() - Dimens.PaddingMedium,
                bottom = innerPadding.calculateBottomPadding()
            )
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(Dimens.SpacerExtraLarge))
        // Search
        TextField(
            value = searchText,
            onValueChange = { newText -> searchText = newText },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.PaddingLarge)
                .clip(RoundedCornerShape(Dimens.CornerRadiusExtraLarge)),
            label = { Text("Search") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    modifier = Modifier.padding(start = Dimens.PaddingSmall),
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search icon"
                )
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(Dimens.SpacerMedium))

        // LazyColumn for the scrollable list of "Recent Saved" section
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
            contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge)
        ) {
            item {
                if (filteredSavedResults.isEmpty()) {
                    Text(
                        text = "No saved item found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Dimens.PaddingMedium)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraSmall)
                    ) {
                        filteredSavedResults.forEachIndexed { index, resultCard ->
                            ResultCard(resultCard)
                            if (index < filteredSavedResults.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookmarkPreview() {
    MLAppTheme {
        Column {
            // For preview purposes, we'll pass a dummy lambda for onClearAllClicked
            MyTopAppBar(
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
                onClearAllClicked = { /* Do nothing in preview */ }
            )
            ScrollContent(
                innerPadding = PaddingValues(),
                savedResults = listOf(
                    ResultCardModel(id = 1, title = "Saved Barcode 1", subtitle = "EAN-13", imageUri = null),
                    ResultCardModel(id = 2, title = "Saved QR Code", subtitle = "URL: example.com", imageUri = null),
                    ResultCardModel(id = 3, title = "Saved Image Label", subtitle = "Cat, Animal", imageUri = null)
                )
            )
        }
    }
}