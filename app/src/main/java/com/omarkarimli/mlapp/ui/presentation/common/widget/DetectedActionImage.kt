package com.omarkarimli.mlapp.ui.presentation.common.widget

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun DetectedActionImage(imageUri: Uri?) {
    val context = LocalContext.current
    var showFullscreenImage by remember { mutableStateOf(false) }

    if (imageUri != null && imageUri.toString().isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .listener(
                    onStart = { Log.d("CoilLoad", "Start loading: $imageUri") },
                    onSuccess = { _, _ -> Log.d("CoilLoad", "Success loading: $imageUri") },
                    onError = { _, result -> Log.e("CoilLoad", "Error loading: $imageUri, ${result.throwable.message}") }
                )
                .build(),
            contentDescription = "Picked ActionImage",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(Dimens.BottomSheetImageSize)
                .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                .clickable { showFullscreenImage = true }
        )

        if (showFullscreenImage) {
            Dialog(
                onDismissRequest = { showFullscreenImage = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                FullscreenImageViewer(
                    imageUri = imageUri,
                    onDismiss = { showFullscreenImage = false }
                )
            }
        }
    }
}

@Composable
fun FullscreenImageViewer(
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .listener(
                    onStart = { Log.d("CoilFullscreen", "Start loading: $imageUri") },
                    onSuccess = { _, _ -> Log.d("CoilFullscreen", "Success loading: $imageUri") },
                    onError = { _, result -> Log.e("CoilFullscreen", "Error loading: $imageUri, ${result.throwable.message}") }
                )
                .build(),
            contentDescription = "Fullscreen Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Dimens.PaddingMedium)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(Dimens.IconSizeLarge)
            )
        }
    }
}