@file:OptIn(ExperimentalMaterial3Api::class)

package com.omarkarimli.mlapp.ui.presentation.screen.bookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.widget.ResultCard
import com.omarkarimli.mlapp.ui.presentation.common.widget.SearchLayout
import com.omarkarimli.mlapp.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val viewModel: BookmarkViewModel = hiltViewModel()
    val savedResults = viewModel.savedResults.collectAsLazyPagingItems() // Collect PagingData
    val searchQuery by viewModel.searchQuery.collectAsState() // Observe search query from ViewModel

    // State to control the visibility of the alert dialog
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(
                scrollBehavior = scrollBehavior,
                // Only enable clear all if there are items to clear
                onClearAllClicked = { if (savedResults.itemCount > 0) showClearAllDialog = true }
            )
        }
    ) { innerPadding ->
        // Pass the PagingItems and the search query/setter
        ScrollContent(
            innerPadding = innerPadding,
            savedResults = savedResults,
            searchQuery = searchQuery,
            onSearchQueryChanged = viewModel::setSearchQuery
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Bookmarks?") },
            text = { Text("Are you sure you want to delete all your saved bookmarks? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllSavedResults()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false }
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
                onClick = onClearAllClicked,
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
    savedResults: LazyPagingItems<ResultCardModel>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(
                top = innerPadding.calculateTopPadding() - Dimens.PaddingMedium,
                bottom = innerPadding.calculateBottomPadding()
            )
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(Dimens.SpacerExtraLarge))
        SearchLayout(searchQuery, onSearchQueryChanged)
        Spacer(modifier = Modifier.height(Dimens.SpacerMedium))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // This will now correctly take the remaining height within the fillMaxHeight Column
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
            contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge)
        ) {
            // Using items(count: Int) and manually accessing items from LazyPagingItems
            items(
                count = savedResults.itemCount, // Use itemCount for the total number of items
                key = { index ->
                    // Provide a unique key. It's crucial for performance and state preservation.
                    // Use the item's ID if available, otherwise fall back to index (less ideal).
                    savedResults.peek(index)?.id ?: index
                }
            ) { index ->
                val result = savedResults[index] // Access item by index

                result?.let {
                    ResultCard(it)
                    if (index < savedResults.itemCount - 1) HorizontalDivider()
                }
            }

            // Handle loading states from Paging 3
            savedResults.apply {
                when {
                    loadState.refresh is LoadState.Loading -> {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(), // fills parent size of LazyColumn
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally // centers horizontally
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    loadState.append is LoadState.Loading -> {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally) // Center horizontally in its own item
                            )
                        }
                    }
                    loadState.refresh is LoadState.Error || loadState.append is LoadState.Error -> {
                        item {
                            val error = (loadState.refresh as? LoadState.Error)?.error
                                ?: (loadState.append as? LoadState.Error)?.error
                            Text(
                                text = "Error: ${error?.localizedMessage ?: "Unknown error"}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth() // Fill width
                                    .padding(vertical = Dimens.PaddingMedium)
                                    .wrapContentWidth(Alignment.CenterHorizontally) // Center horizontally
                            )
                        }
                    }
                    // Handle empty states based on loaded items and search query
                    itemCount == 0 && loadState.refresh is LoadState.NotLoading -> {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(), // fills parent size of LazyColumn
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "No items matching \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Dimens.PaddingMedium)
                                    )
                                } else {
                                    Text(
                                        text = "No saved items found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Dimens.PaddingMedium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}