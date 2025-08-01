package com.omarkarimli.mlapp.domain.models

import android.net.Uri

data class RecognizedText(
    val text: String,
    val imageUri: Uri? = null
)
