package com.omarkarimli.mlapp.ui.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.domain.models.ListItemModel
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.ui.common.widget.ListItemUi
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // Sample data for your list
    val items = listOf(
        ListItemModel(
            "1",
            Icons.Rounded.TextFields,
            "Text Recognition",
            "Recognize and extract text from images.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.TextRecognition.route) }
        ),
        ListItemModel(
            "2",
            Icons.Rounded.Face,
            "Face Mesh Detection",
            "Detect face mesh info on close-range images.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.FaceMeshDetection.route) }
        ),
        ListItemModel(
            "3",
            Icons.Rounded.ImageSearch,
            "Image Labeling",
            "Identify objects, locations, activities, animal species, products, and more.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.ImageLabeling.route) }
        ),
        ListItemModel(
            "4",
            Icons.Rounded.QrCodeScanner,
            "Barcode Scanning",
            "Scan and process barcodes. Supports most standard 1D and 2D formats.",
            Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { navController.navigate(Screen.BarcodeScanning.route) }
        ),
        ListItemModel(
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
        ScrollContent(innerPadding, navController, items)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(scrollBehavior: TopAppBarScrollBehavior, items: List<ListItemModel>) {
    var expanded by remember { mutableStateOf(false) }
    val isTopAppBarMinimized = scrollBehavior.state.collapsedFraction > 0.5 // Adjust threshold as needed
    MediumTopAppBar(
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            Row {
                Spacer(Modifier.size(Dimens.PaddingSmall))
                FilledIconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(Dimens.IconSizeLarge), // Changed from 32.dp
                    shape = IconButtonDefaults.filledShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.MoreHoriz,
                        contentDescription = "Menu",
                        modifier = Modifier.size(Dimens.IconSizeMedium) // Changed from 24.dp
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
                            // Handle item click, e.g., navigate to a specific screen
                            // For now, just close the menu
                            expanded = false
                            println("Clicked: ${item.title}") // For demonstration
                        }
                    )
                }
            }
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
                    imageVector = Icons.Rounded.CameraAlt,
                    modifier = Modifier.size(Dimens.IconSizeSmall), // Changed from 20.dp
                    contentDescription = "Open Camera"
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
                    Icons.Rounded.Add,
                    modifier = Modifier.size(Dimens.IconSizeSmall), // Changed from 20.dp
                    contentDescription = "Add new collab"
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall)) // Changed from 8.dp
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues,
    navController: NavHostController,
    items: List<ListItemModel>
) {
    Column(
        modifier = Modifier
            .padding(
                top = innerPadding.calculateTopPadding() - Dimens.PaddingMedium, // Adjusted to use Dimens.PaddingLarge
                bottom = innerPadding.calculateBottomPadding()
            ) // Apply innerPadding to the whole column
            .fillMaxWidth()
    ) {
        // Search bar takes up its natural height
        CustomizableSearchBar(
            modifier = Modifier
                .fillMaxWidth(),
            query = "",
            onQueryChange = {},
            onSearch = {},
            searchResults = listOf("Item 1", "Item 2", "Item 3"),
            onResultClick = { /* Handle result click */ },
        )

        Spacer(modifier = Modifier.height(Dimens.SpacerMedium)) // Changed from 16.dp

        // LazyColumn for the scrollable list - now it takes the remaining space
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall) // Changed from 8.dp
        ) {
            items(items) { item ->
                ListItemUi(item = item)
            }
        }

        Spacer(modifier = Modifier.height(Dimens.PaddingLarge)) // Changed from 24.dp
        Recent()
    }
}

@Composable
private fun Recent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PaddingLarge), // Changed from 24.dp
    ) {
        Text(
            text = "Recent",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Dimens.PaddingSmall)) // Changed from 8.dp
        Text(
            text = "No item found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizableSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    onResultClick: (String) -> Unit,
    // Customization options
    placeholder: @Composable () -> Unit = { Text("Search") },
    leadingIcon: @Composable (() -> Unit)? = { Icon(Icons.Default.Search, contentDescription = "Search") },
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingContent: (@Composable (String) -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    // Track expanded state of search bar
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier // Remove fillMaxSize() here
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                // Customizable input field implementation
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = {
                        onSearch(query)
                        expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            // Show search results in a lazy column for better performance
            LazyColumn {
                items(count = searchResults.size) { index ->
                    val resultText = searchResults[index]
                    ListItem(
                        headlineContent = { Text(resultText) },
                        supportingContent = supportingContent?.let { { it(resultText) } },
                        leadingContent = leadingContent,
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .clickable {
                                onResultClick(resultText)
                                expanded = false
                            }
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimens.PaddingMedium, // Changed from 16.dp
                                vertical = Dimens.PaddingExtraSmall // Changed from 4.dp
                            )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    MLAppTheme {
        HomeScreen(navController = NavHostController(LocalContext.current))
    }
}