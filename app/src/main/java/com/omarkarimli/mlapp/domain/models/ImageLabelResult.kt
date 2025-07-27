package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.label.ImageLabel

data class ImageLabelResult(
    val label: ImageLabel,
    val imageUri: Uri? = null // Nullable for live scans
)

fun List<ImageLabelResult>.toResultCards(): List<ResultCardModel> {
    return this.map { imageLabelResult ->
        ResultCardModel(
            title = imageLabelResult.label.text,
            subtitle = "Confidence: ${imageLabelResult.label.confidence.toString()}",
            imageUri = imageLabelResult.imageUri
        )
    }
}