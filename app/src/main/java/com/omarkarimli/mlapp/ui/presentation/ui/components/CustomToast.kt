package com.omarkarimli.mlapp.ui.presentation.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.omarkarimli.mlapp.utils.Dimens
import com.omarkarimli.mlapp.utils.Dimens.textAnimationDelayMillis
import com.omarkarimli.mlapp.utils.Dimens.textAnimationDurationMillis
import com.omarkarimli.mlapp.utils.Dimens.toastEntryExitDurationMillis
import kotlinx.coroutines.delay

// Enum for custom toast types
enum class CustomToastType {
    INFO, SUCCESS, ERROR
}

// Data class to hold custom toast state
data class CustomToastState(
    val message: String? = null,
    val type: CustomToastType,
    val isVisible: Boolean = false
)

/**
 * A composable function to display a custom toast with animation, icon, and rounded corners.
 * This function observes a [MutableState<CustomToastState>] to control its visibility and content.
 *
 * @param toastState The mutable state holding the current toast information.
 * @param modifier The modifier to be applied to the toast.
 * @param textAnimationDelayMillis The delay in milliseconds before the text animation starts.
 * @param textAnimationDurationMillis The duration of the text slide-in animation in milliseconds.
 * @param toastEntryExitDurationMillis The duration of the toast's overall entry and exit animations.
 */
@Composable
fun CustomToast(
    toastState: MutableState<CustomToastState>,
    modifier: Modifier = Modifier
) {
    Log.d("CustomToast", "CustomToast called with message: ${toastState.value.message}")

    // State to control the text's visibility for its specific animation
    var animateText by remember { mutableStateOf(false) }

    // LaunchedEffect to control the text animation sequence
    LaunchedEffect(toastState.value.isVisible, toastState.value.message) {
        // Reset animateText when toast visibility changes or message changes
        // This ensures the animation replays if the message updates while visible
        if (toastState.value.isVisible) {
            animateText = false // Hide text immediately to prepare for new animation
            delay(textAnimationDelayMillis)
            animateText = true
        } else {
            animateText = false // Hide text immediately when toast is dismissed
        }
    }

    AnimatedVisibility(
        visible = toastState.value.isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = toastEntryExitDurationMillis)
        ) + fadeIn(animationSpec = tween(durationMillis = toastEntryExitDurationMillis)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = toastEntryExitDurationMillis)
        ) + fadeOut(animationSpec = tween(durationMillis = toastEntryExitDurationMillis))
    ) {
        val currentToast = toastState.value
        val backgroundColor = when (currentToast.type) {
            CustomToastType.INFO -> MaterialTheme.colorScheme.primaryContainer
            CustomToastType.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
            CustomToastType.ERROR -> MaterialTheme.colorScheme.errorContainer
        }
        val contentColor = when (currentToast.type) {
            CustomToastType.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
            CustomToastType.SUCCESS -> MaterialTheme.colorScheme.onTertiaryContainer
            CustomToastType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        }
        val icon = when (currentToast.type) {
            CustomToastType.INFO -> Icons.Rounded.Info
            CustomToastType.SUCCESS -> Icons.Rounded.CheckCircle
            CustomToastType.ERROR -> Icons.Rounded.Error
        }

        Box(
            modifier = modifier
        ) {
            Card(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMedium)
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.PaddingSmall).animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = contentColor)

                    if (!currentToast.message.isNullOrEmpty()) {
                        // Text with delayed slide-in animation
                        AnimatedVisibility(
                            visible = animateText,
                            enter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(durationMillis = textAnimationDurationMillis)
                            ) + fadeIn(animationSpec = tween(durationMillis = textAnimationDurationMillis)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 0))
                        ) {
                            Text(
                                text = currentToast.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                                    .padding(horizontal = Dimens.PaddingSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}