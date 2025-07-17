package com.omarkarimli.mlapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable // This annotation is crucial
fun Dp.toPxCustom(): Float {
    return with(LocalDensity.current) { this@toPxCustom.toPx() }
}