package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.label.ImageLabel

data class ImageLabelResult(
    val label: ImageLabel,
    val imageUri: Uri? = null // Nullable for live scans
)