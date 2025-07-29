package com.omarkarimli.mlapp.ui.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.ui.navigation.Screen

@Composable
fun SplashScreen(navController: NavHostController) {
    // Get the current context to initialize ExoPlayer
    val context = LocalContext.current

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Replace with your video URI. For a local video, place it in res/raw/ and use:
            // MediaItem.fromUri("android.resource://${context.packageName}/raw/sample_video")
            // For a network video, use:
            // MediaItem.fromUri("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            setMediaItem(MediaItem.fromUri("android.resource://${context.packageName}/raw/splash_video"))
            prepare()
            playWhenReady = true

            // Add a listener to detect when the video playback ends
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Video has ended
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            })
        }
    }

    // Manage ExoPlayer lifecycle with the Composable lifecycle
    // This ensures the player is paused/resumed and released correctly.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, key2 = exoPlayer) { // Add exoPlayer to key1 to ensure listener cleanup
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Cleanup: remove observer and release player when the Composable leaves the composition
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE2E0F9)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // DON'T Show playback controls
                }
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}
