package com.omarkarimli.mlapp.ui.presentation.components

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun ActionImage(context: Context, imageUri: Uri?) {
    var showFullscreenImage by remember { mutableStateOf(false) }

    imageUri?.let { uri ->
        val bitmap = remember(uri) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                Log.e("ActionImage", "Error loading image for card: ${e.message}", e)
                null
            }
        }

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Picked ActionImage",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(Dimens.BottomSheetImageSize)
                    .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                    .clickable { showFullscreenImage = true } // Make image clickable
            )

            if (showFullscreenImage) {
                // Use Dialog for true fullscreen overlay
                Dialog(
                    onDismissRequest = { showFullscreenImage = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false) // Important for fullscreen
                ) {
                    FullscreenImageViewer(
                        imageUri = uri,
                        context = context,
                        onDismiss = { showFullscreenImage = false }
                    )
                }
            }
        } ?: run {
            ImageLoadErrorPlaceholder(modifier = Modifier.size(Dimens.BottomSheetImageSize))
        }
    }
}

@Composable
fun ImageLoadErrorPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Photo,
            contentDescription = "No Image",
            tint = Color.Gray
        )
    }
}

@Composable
fun FullscreenImageViewer(
    imageUri: Uri,
    context: Context,
    onDismiss: () -> Unit
) {
    val bitmap = remember(imageUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
        } catch (e: Exception) {
            Log.e("FullscreenImageViewer", "Error loading fullscreen image: ${e.message}", e)
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Fullscreen Image",
                contentScale = ContentScale.Fit, // Use Fit to show the whole image
                modifier = Modifier
                    .fillMaxSize() // Fills the dialog's content area
                    .padding(Dimens.PaddingSmall) // Optional padding around the image
            )
        } ?: run {
            // Placeholder for error loading fullscreen image
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = "Could not load image",
                    tint = Color.LightGray,
                    modifier = Modifier.size(Dimens.BottomSheetImageSize)
                )
            }
        }

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