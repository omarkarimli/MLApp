package com.omarkarimli.mlapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
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
import com.omarkarimli.mlapp.ui.presentation.BarcodeScanningScreen
import com.omarkarimli.mycollab.ui.presentation.BookmarkScreen
import com.omarkarimli.mlapp.ui.presentation.HomeScreen
import com.omarkarimli.mlapp.ui.presentation.ImageLabelingScreen
import com.omarkarimli.mlapp.ui.presentation.LoginScreen
import com.omarkarimli.mlapp.ui.presentation.ObjectDetectionScreen
import com.omarkarimli.mlapp.ui.presentation.OnboardingScreen
import com.omarkarimli.mlapp.ui.presentation.SettingsScreen
import com.omarkarimli.mlapp.ui.presentation.SplashScreen
import com.omarkarimli.mlapp.ui.presentation.TextRecognitionScreen

// Define a sealed class for bottom navigation destinations
sealed class BottomBarDestination(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val contentDescription: String
) {
    data object Home : BottomBarDestination("home", Icons.Rounded.Home, "Home", "Home Screen")
    data object Bookmarks : BottomBarDestination("bookmarks", Icons.Rounded.Bookmark, "Bookmarks", "Bookmarks Screen")
    data object Settings : BottomBarDestination("settings", Icons.Rounded.Settings, "Settings", "Settings Screen")
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
                                // Navigate to the selected destination, handling back stack
                                navController.navigate(destination.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
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
                com.omarkarimli.mlapp.ui.presentation.FaceMeshDetectionScreen(navController = navController)
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