package com.omarkarimli.mycollab.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.ui.presentation.ui.ListItemModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // Sample data for your list
    val items = listOf(
        ListItemModel(
            "1",
            Icons.Rounded.TextFields,
            "Text Recognition",
            "Recognize and extract text from images.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "2",
            Icons.Rounded.Face,
            "Face Mesh Detection",
            "Detect face mesh info on close-range images.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "3",
            Icons.Rounded.ImageSearch,
            "Image Labeling",
            "Identify objects, locations, activities, animal species, products, and more.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "4",
            Icons.Rounded.QrCodeScanner,
            "Barcode Scanning",
            "Scan and process barcodes. Supports most standard 1D and 2D formats.",
            Icons.AutoMirrored.Rounded.ArrowForward
        ),
        ListItemModel(
            "5",
            Icons.Rounded.CenterFocusStrong,
            "Object Detection",
            "Localize and track in real time one or more objects in the live camera feed.",
            Icons.AutoMirrored.Rounded.ArrowForward
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
            Text(
                "Bookmarks",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isTopAppBarMinimized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            Row {
                Spacer(Modifier.size(8.dp))
                FilledIconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp),
                    shape = IconButtonDefaults.filledShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.MoreHoriz,
                        contentDescription = "Menu",
                        modifier = Modifier.size(24.dp)
                    )
                }
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
                modifier = Modifier.size(40.dp),
                shape = IconButtonDefaults.filledShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Open Camera"
                )
            }
            Spacer(Modifier.size(8.dp))
            FilledTonalIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.width(50.dp).height(40.dp),
                shape = IconButtonDefaults.filledShape
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add new collab",
                )
            }
            Spacer(Modifier.size(8.dp))
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
                top = innerPadding.calculateTopPadding() - 24.dp,
                bottom = innerPadding.calculateBottomPadding()
            ) // Apply innerPadding to the whole column
            .fillMaxWidth()
    ) {
        Text(
            text = "No item found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}