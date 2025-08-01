package com.omarkarimli.mlapp.domain.models

import androidx.compose.ui.graphics.vector.ImageVector

data class StandardListItemModel(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val endingIcon: ImageVector,
    val onClick: () -> Unit = {}
)