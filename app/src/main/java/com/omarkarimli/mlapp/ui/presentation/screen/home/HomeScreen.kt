@file:OptIn(ExperimentalMaterial3Api::class)

package com.omarkarimli.mlapp.ui.presentation.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.domain.models.StandardListItemModel
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.widget.StandardListItemUi
import com.omarkarimli.mlapp.utils.Dimens
import androidx.hilt.navigation.compose.hiltViewModel
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.ui.presentation.common.widget.OriginalResultCard
import com.omarkarimli.mlapp.ui.presentation.common.widget.SearchLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val viewModel: HomeViewModel = hiltViewModel()
    val savedResults by viewModel.savedResults.collectAsState()

    // Sample data for your list (unchanged)
    val items = listOf(
        StandardListItemModel(
            "1",
            Icons.Rounded.TextFields,
            "Text Recognition",
            "Recognize and extract text from images.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.TextRecognition.route) }
        ),
        StandardListItemModel(
            "2",
            Icons.Rounded.Face,
            "Face Mesh Detection",
            "Detect face mesh info on close-range images.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.FaceMeshDetection.route) }
        ),
        StandardListItemModel(
            "3",
            Icons.Rounded.ImageSearch,
            "Image Labeling",
            "Identify objects, locations, activities, animal species, products, and more.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.ImageLabeling.route) }
        ),
        StandardListItemModel(
            "4",
            Icons.Rounded.QrCodeScanner,
            "Barcode Scanning",
            "Scan and process barcodes. Supports most standard 1D and 2D formats.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.BarcodeScanning.route) }
        ),
        StandardListItemModel(
            "5",
            Icons.Rounded.CenterFocusStrong,
            "Object Detection",
            "Localize and track in real time one or more objects in the live camera feed.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.ObjectDetection.route) }
        )
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(scrollBehavior, items)
        }
    ) { innerPadding ->
        ScrollContent(innerPadding, items, savedResults) // Pass savedResults
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(scrollBehavior: TopAppBarScrollBehavior, items: List<StandardListItemModel>) {
    var expanded by remember { mutableStateOf(false) }
    val isTopAppBarMinimized = scrollBehavior.state.collapsedFraction > 0.5 // Adjust threshold as needed
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
            ) {
                AnimatedVisibility(
                    visible = !isTopAppBarMinimized,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    Spacer(Modifier.size(Dimens.PaddingSmall))
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(Dimens.IconSizeExtraLarge)
                    )
                }
                Text(
                    stringResource(R.string.app_name),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (isTopAppBarMinimized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        navigationIcon = {
            Row {
                Spacer(Modifier.size(Dimens.PaddingSmall))
                FilledIconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(Dimens.IconSizeLarge),
                    shape = IconButtonDefaults.filledShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.MoreHoriz,
                        contentDescription = "Menu",
                        modifier = Modifier.size(Dimens.IconSizeMedium)
                    )
                }
                Spacer(Modifier.size(Dimens.PaddingSmall))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Dynamically display items from the list
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.title) },
                        leadingIcon = { Icon(item.icon, contentDescription = null) },
                        onClick = {
                            item.onClick() // Call the onClick lambda
                            expanded = false
                        }
                    )
                }
            }
        },
        actions = {
            FilledIconButton(
                onClick = { /* do something */ },
                modifier = Modifier.size(Dimens.IconSizeLarge),
                shape = IconButtonDefaults.filledShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    modifier = Modifier.size(Dimens.IconSizeSmall),
                    contentDescription = "Open Camera"
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall))
            FilledTonalIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier
                    .width(Dimens.IconSizeExtraLarge)
                    .height(Dimens.IconSizeLarge),
                shape = IconButtonDefaults.filledShape
            ) {
                Icon(
                    Icons.Rounded.Add,
                    modifier = Modifier.size(Dimens.IconSizeSmall),
                    contentDescription = "Add new collab"
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall))
        }
    )
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues,
    items: List<StandardListItemModel>,
    savedResults: List<ResultCardModel>
) {
    var searchText by remember { mutableStateOf("") }

    val filteredItems = remember(searchText, items) {
        if (searchText.isBlank()) {
            items
        } else {
            items.filter {
                it.title.contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
            }
        }
    }

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
        SearchLayout(searchText) { newText -> searchText = newText }
        Spacer(modifier = Modifier.height(Dimens.SpacerMedium))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Give it weight to fill remaining space
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
            contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge) // Apply horizontal padding here for list items
        ) {
            items(filteredItems) { item -> // Use filteredItems here
                StandardListItemUi(item = item)
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
                RecentlySaved(filteredSavedResults)
                Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            }
        }
    }
}

@Composable
private fun RecentlySaved(filteredSavedResults: List<ResultCardModel>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Recently Saved",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimens.PaddingSmall) // Add bottom padding for the title
        )

        if (filteredSavedResults.isEmpty()) {
            Text(
                text = "No saved item found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.PaddingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                filteredSavedResults.forEachIndexed { index, resultCard ->
                    OriginalResultCard(resultCard)
                }
            }
        }
    }
}