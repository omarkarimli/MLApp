package com.omarkarimli.mlapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omarkarimli.mlapp.ui.presentation.screen.facemeshdetection.FaceMeshDetectionScreen
import com.omarkarimli.mlapp.ui.presentation.screen.bookmark.BookmarkScreen
import com.omarkarimli.mlapp.ui.presentation.screen.home.HomeScreen
import com.omarkarimli.mlapp.ui.presentation.screen.imagelabeling.ImageLabelingScreen
import com.omarkarimli.mlapp.ui.presentation.screen.LoginScreen
import com.omarkarimli.mlapp.ui.presentation.screen.objectdetection.ObjectDetectionScreen
import com.omarkarimli.mlapp.ui.presentation.screen.OnboardingScreen
import com.omarkarimli.mlapp.ui.presentation.screen.SettingsScreen
import com.omarkarimli.mlapp.ui.presentation.screen.splash.SplashScreen
import com.omarkarimli.mlapp.ui.presentation.screen.barcodescanning.BarcodeScanningScreen
import com.omarkarimli.mlapp.ui.presentation.screen.textrecognition.TextRecognitionScreen

sealed class BottomBarDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val contentDescription: String
) {
    data object Home : BottomBarDestination("home", Icons.Rounded.Home, Icons.Outlined.Home, "Home", "Home Screen")
    data object Bookmarks : BottomBarDestination("bookmarks", Icons.Rounded.Bookmark, Icons.Outlined.BookmarkBorder, "Bookmarks", "Bookmarks Screen")
    data object Settings : BottomBarDestination("settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "Settings", "Settings Screen")
}

val bottomBarDestinations = listOf(
    BottomBarDestination.Home,
    BottomBarDestination.Bookmarks,
    BottomBarDestination.Settings
)

@Composable
fun AppNavigation() {
    // Create a NavController to manage navigation state
    val navController = rememberNavController()

    // Determine the currently selected bottom navigation item
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Scaffold provides the basic visual structure for Material Design components
    Scaffold(
        bottomBar = {
            // Conditionally show the NavigationBar based on the current route
            val showBottomBar = bottomBarDestinations.any { it.route == currentRoute }
            if (showBottomBar) {
                // Use NavigationBar for modern bottom navigation
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomBarDestinations.forEach { destination ->
                        val isSelected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.contentDescription
                                )
                            },
                            // label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // NavHost defines the navigation graph and manages screen content
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(
                bottom = paddingValues.calculateBottomPadding()
            ) // Apply padding from Scaffold
        ) {
            // Composable for the Splash screen
            composable(Screen.Splash.route) {
                SplashScreen(navController = navController)
            }
            // Composable for the Onboarding screen
            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController = navController)
            }
            // Composable for the Login screen
            composable(Screen.Login.route) {
                LoginScreen(navController = navController)
            }
            // Composable for the Home screen
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            // Composable for the Bookmarks screen
            composable(Screen.Bookmarks.route) {
                BookmarkScreen(navController = navController)
            }
            // Composable for the Settings screen
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            // Composable for the Barcode Scanning screen
            composable(Screen.BarcodeScanning.route) {
                BarcodeScanningScreen(navController = navController)
            }
            // Composable for the Image Labeling screen
            composable(Screen.ImageLabeling.route) {
                ImageLabelingScreen(navController = navController)
            }
            // Composable for the Text Recognition screen
            composable(Screen.TextRecognition.route) {
                TextRecognitionScreen(navController = navController)
            }
            // Composable for the Object Detection screen
            composable(Screen.ObjectDetection.route) {
                ObjectDetectionScreen(navController = navController)
            }
            // Composable for the Face Mesh Detection screen
            composable(Screen.FaceMeshDetection.route) {
                FaceMeshDetectionScreen(navController = navController)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Bookmarks : Screen("bookmarks", "Bookmarks")
    data object Settings : Screen("settings", "Settings")
    data object Onboarding: Screen("onboarding", "Onboarding")
    data object Login: Screen("login", "Login")
    data object Splash: Screen("splash", "Splash")
    data object BarcodeScanning: Screen("barcode_scanning", "Barcode Scanning")
    data object ImageLabeling: Screen("image_labeling", "Image Labeling")
    data object TextRecognition: Screen("text_recognition", "Text Recognition")
    data object ObjectDetection: Screen("object_detection", "Object Detection")
    data object FaceMeshDetection: Screen("face_mesh_detection", "Face Mesh Detection")
}