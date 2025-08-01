package com.omarkarimli.mlapp.ui.presentation.common.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clipToBounds
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun SwipeToRevealBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    revealedContent: @Composable () -> Unit,
    revealedContentWidth: Dp
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val revealedWidthPx = with(LocalDensity.current) { revealedContentWidth.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(
                topEnd = Dimens.CornerRadiusMedium,
                bottomEnd = Dimens.CornerRadiusMedium
            ))
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .width(revealedContentWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .offset {
                    IntOffset(x = (revealedWidthPx + offsetX.value).roundToInt(), y = 0)
                }
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            revealedContent()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < -revealedWidthPx / 2) {
                                    offsetX.animateTo(-revealedWidthPx, animationSpec = tween(200))
                                } else {
                                    offsetX.animateTo(0f, animationSpec = tween(200))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, animationSpec = tween(200))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-revealedWidthPx, 0f)
                            coroutineScope.launch {
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun OriginalResultCard(resultCardModel: ResultCardModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(
                topEnd = Dimens.CornerRadiusMedium,
                bottomEnd = Dimens.CornerRadiusMedium
            ))
            .padding(horizontal = Dimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicator
        Box(
            modifier = Modifier
                .height(Dimens.IndicatorHeight)
                .width(Dimens.IndicatorWidth)
                .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                .background(MaterialTheme.colorScheme.primary)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.PaddingMedium)
        ) {
            Text(
                resultCardModel.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                resultCardModel.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DetectedActionImage(imageUri = resultCardModel.imageUri)
    }
}

@Composable
private fun SwipeActionItem(
    modifier: Modifier,
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(Dimens.PaddingSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = text, tint = contentColor)
        Spacer(Modifier.height(Dimens.SpacerSmall))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

@Composable
fun SwipeableResultCard(resultCardModel: ResultCardModel, onDelete: () -> Unit, onInfo: () -> Unit) {
    SwipeToRevealBox(
        revealedContentWidth = Dimens.RevealedWidth,
        content = {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                OriginalResultCard(resultCardModel = resultCardModel)
            }
        },
        revealedContent = {
            Row(
                modifier = Modifier
                    .width(Dimens.RevealedWidth)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SwipeActionItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Info,
                    text = "Info",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onInfo
                )
                SwipeActionItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Close,
                    text = "Delete",
                    backgroundColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = onDelete
                )
            }
        }
    )
}