package com.omarkarimli.mlapp.domain.models

import androidx.compose.ui.graphics.vector.ImageVector

data class StandardListItemModel(
    val id: Int,
    val leadingIcon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val endingIcon: ImageVector? = null,
    val bgIcon: ImageVector? = null,
    val onClick: () -> Unit = {}
)