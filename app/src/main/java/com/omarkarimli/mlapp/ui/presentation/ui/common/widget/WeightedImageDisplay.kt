package com.omarkarimli.mlapp.ui.presentation.ui.common.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive // Import isActive

@Composable
fun WeightedImageDisplay(modifier: Modifier = Modifier) {
    val selectedIndex = remember { mutableIntStateOf(0) }
    val isAnimationPlaying = remember { mutableStateOf(true) }

    val weights = remember {
        listOf(
            Animatable(1f),
            Animatable(1f),
            Animatable(1f)
        )
    }

    // Initialize weights only once when the composable is first launched
    LaunchedEffect(Unit) {
        weights[0].snapTo(2f)
        weights[1].snapTo(1.5f)
        weights[2].snapTo(0.5f)
    }

    LaunchedEffect(isAnimationPlaying.value) {
        if (isAnimationPlaying.value) {
            // Start from the current selectedIndex if resuming
            var currentIndex = selectedIndex.intValue

            while (isActive && isAnimationPlaying.value) { // Check isActive for coroutine cancellation safety
                selectedIndex.intValue = currentIndex

                weights.forEachIndexed { index, animatableWeight ->
                    val targetWeight = if (index == currentIndex) 2f else 0.5f
                    // Only animate if the target is different
                    if (animatableWeight.value != targetWeight) {
                        animatableWeight.animateTo(
                            targetValue = targetWeight,
                            animationSpec = tween(durationMillis = Dimens.animDurationMedium, easing = LinearEasing)
                        )
                    }
                }
                delay(2000)

                // Move to the next index, cycling back to 0
                currentIndex = (currentIndex + 1) % weights.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacerMedium)
        ) {
            val images = listOf(
                painterResource(id = R.drawable.g1),
                painterResource(id = R.drawable.g2),
                painterResource(id = R.drawable.g3)
            )

            images.forEachIndexed { index, imageResource ->
                val currentWeight by weights[index].asState()

                Box(
                    modifier = Modifier
                        .weight(currentWeight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(Dimens.CornerRadiusExtraLarge))
                ) {
                    Image(
                        painter = imageResource,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        FilledIconButton(
            onClick = { isAnimationPlaying.value = !isAnimationPlaying.value },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = Dimens.PaddingSmall, end = Dimens.PaddingExtraSmall)
                .size(Dimens.IconSizeExtraLarge),
            shape = IconButtonDefaults.filledShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isAnimationPlaying.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                contentColor = if (isAnimationPlaying.value) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = if (isAnimationPlaying.value) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isAnimationPlaying.value) "Pause Animation" else "Play Animation",
                modifier = Modifier.size(Dimens.IconSizeSmall)
            )
        }
    }
}