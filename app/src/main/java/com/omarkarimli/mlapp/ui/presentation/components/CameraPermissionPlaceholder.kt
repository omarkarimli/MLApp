package com.omarkarimli.mlapp.ui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun CameraPermissionPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(Dimens.CornerRadiusMedium)),
        contentAlignment = Alignment.Center
    ) {
        Text("Camera permission not granted", color = Color.White)
    }
}