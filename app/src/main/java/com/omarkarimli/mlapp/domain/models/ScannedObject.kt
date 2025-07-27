package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.objects.DetectedObject

data class ScannedObject(
    val detectedObject: DetectedObject,
    val imageUri: Uri? = null // Null for live scan, non-null for picked image
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScannedObject

        return detectedObject == other.detectedObject
    }

    override fun hashCode(): Int {
        return detectedObject.hashCode()
    }
}

fun List<ScannedObject>.toResultCards(): List<ResultCardModel> {
    return this.map { scannedObject ->
        val title: String = scannedObject.detectedObject.labels.firstOrNull()?.text
            ?: "Detected Object" // Provide a default if no labels are present

        val subtitle: String = "${scannedObject.detectedObject.trackingId}: ${scannedObject.detectedObject.boundingBox.width()}x${scannedObject.detectedObject.boundingBox.height()}"

        ResultCardModel(
            title = title,
            subtitle = subtitle,
            imageUri = scannedObject.imageUri
        )
    }
}