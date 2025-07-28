package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.objects.DetectedObject
import com.omarkarimli.mlapp.utils.Constants

data class ScannedObject(
    val detectedObject: DetectedObject,
    val imageUri: Uri? = null // Null for live scan, non-null for picked image
)

fun List<ScannedObject>.toResultCards(): List<ResultCardModel> {
    return this.map { scannedObject ->
        val title: String = scannedObject.detectedObject.labels.firstOrNull()?.text ?: Constants.NOT_APPLICABLE
        val subtitle = "${scannedObject.detectedObject.boundingBox.width()}x${scannedObject.detectedObject.boundingBox.height()}"

        ResultCardModel(
            title = title,
            subtitle = subtitle,
            imageUri = scannedObject.imageUri
        )
    }
}