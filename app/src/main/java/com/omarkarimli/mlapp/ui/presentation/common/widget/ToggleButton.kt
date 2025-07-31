package com.omarkarimli.mlapp.ui.presentation.common.widget

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
        modifier = Modifier.size(Dimens.IconSizeLarge),
        shape = IconButtonDefaults.filledShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = if (isToggled) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isToggled) "Pause Camera" else "Play Camera",
            modifier = Modifier.size(Dimens.IconSizeSmall)
        )
    }
}