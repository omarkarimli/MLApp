package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.objects.DetectedObject

data class ScannedObject(
    val detectedObject: DetectedObject,
    val imageUri: Uri? = null // Null for live scan, non-null for picked image
)