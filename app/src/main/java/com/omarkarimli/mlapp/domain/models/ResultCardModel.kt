package com.omarkarimli.mlapp.domain.models

import android.net.Uri

data class ResultCardModel(
    val title: String,
    val subtitle: String,
    val imageUri: Uri? = null
)