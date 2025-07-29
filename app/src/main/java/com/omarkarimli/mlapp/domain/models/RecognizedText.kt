package com.omarkarimli.mlapp.domain.models

import android.net.Uri

data class RecognizedText(
    val text: String,
    val imageUri: Uri? = null // Nullable: null for live scans, non-null for picked images
)
