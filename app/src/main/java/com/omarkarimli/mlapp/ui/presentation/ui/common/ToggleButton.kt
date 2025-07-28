package com.omarkarimli.mlapp.ui.presentation.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun ToggleButton(
    isToggled: Boolean,
    onToggle: () -> Unit
) {
    FilledIconButton(
        onClick = onToggle,
        modifier = Modifier.size(Dimens.IconSizeLarge), // Apply size modifier here
        shape = IconButtonDefaults.filledShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isToggled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.onSurface,
            contentColor = if (isToggled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        if (isToggled) {
            Icon(
                imageVector = Icons.Filled.Pause,
                contentDescription = "Pause Camera",
                modifier = Modifier.size(Dimens.IconSizeSmall)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play Camera",
                modifier = Modifier.size(Dimens.IconSizeSmall)
            )
        }
    }
}