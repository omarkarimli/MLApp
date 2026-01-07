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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.DpOffset
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

    val items = listOf(
        StandardListItemModel(
            0,
            leadingIcon = Icons.Rounded.TextFields,
            title = "Text Recognition",
            description = "Recognize and extract text from images.",
            endingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.TextRecognition.route) }
        ),
        StandardListItemModel(
            1,
            leadingIcon = Icons.Rounded.Face,
            title = "Face Mesh Detection",
            description = "Detect face mesh info on close-range images.",
            endingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.FaceMeshDetection.route) }
        ),
        StandardListItemModel(
            2,
            leadingIcon = Icons.Rounded.ImageSearch,
            title = "Image Labeling",
            description = "Identify objects, locations, activities, animal species, products, and more.",
            endingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.ImageLabeling.route) }
        ),
        StandardListItemModel(
            3,
            leadingIcon = Icons.Rounded.QrCodeScanner,
            title = "Barcode Scanning",
            description = "Scan and process barcodes. Supports most standard 1D and 2D formats.",
            endingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.BarcodeScanning.route) }
        ),
        StandardListItemModel(
            4,
            leadingIcon = Icons.Rounded.CenterFocusStrong,
            title = "Object Detection",
            description = "Localize and track in real time one or more objects in the live camera feed.",
            endingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.ObjectDetection.route) }
        )
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { MyTopAppBar(scrollBehavior, items) }
    ) { innerPadding ->
        ScrollContent(innerPadding, items, savedResults)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(scrollBehavior: TopAppBarScrollBehavior, items: List<StandardListItemModel>) {
    var expanded by remember { mutableStateOf(false) }
    val isTopAppBarMinimized = scrollBehavior.state.collapsedFraction > 0.5

    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(Dimens.IconSizeLarge)
                )

                AnimatedVisibility(
                    visible = !isTopAppBarMinimized,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    Text(
                        modifier = Modifier.padding(start = Dimens.PaddingSmall),
                        text = stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            FilledTonalIconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .width(Dimens.IconSizeExtraLarge)
                    .height(Dimens.IconSizeLarge),
                shape = IconButtonDefaults.filledShape
            ) {
                Icon(
                    if (expanded) Icons.Rounded.Close else Icons.Rounded.Add,
                    modifier = Modifier.size(Dimens.IconSizeSmall),
                    contentDescription = "Add new collab"
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall))

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                offset = DpOffset(-Dimens.PaddingSmall, Dimens.PaddingSmall)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.title) },
                        leadingIcon = { Icon(item.leadingIcon, contentDescription = null) },
                        onClick = {
                            item.onClick()
                            expanded = false
                        }
                    )
                }
            }
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
                it.title.contains(searchText, ignoreCase = true) || it.description?.contains(searchText, ignoreCase = true) == true
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
                top = innerPadding.calculateTopPadding() - Dimens.PaddingMedium
            )
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(Dimens.SpacerExtraLarge))
        SearchLayout(searchText) { newText -> searchText = newText }
        Spacer(modifier = Modifier.height(Dimens.SpacerMedium))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
            contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge)
        ) {
            items(filteredItems) { item ->
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Recently Saved",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimens.PaddingSmall)
        )

        if (filteredSavedResults.isEmpty()) {
            Text(
                text = "No saved item found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.PaddingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                items(filteredSavedResults) { resultCard ->
                    OriginalResultCard(resultCard)
                }
            }
        }
    }
}