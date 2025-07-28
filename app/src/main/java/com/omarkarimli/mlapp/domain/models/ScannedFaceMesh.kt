package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.facemesh.FaceMesh

data class ScannedFaceMesh(
    val faceMesh: FaceMesh,
    val imageUri: Uri? = null // Nullable for live scans
)

fun List<ScannedFaceMesh>.toResultCards(): List<ResultCardModel> {
    return this.map { scannedFaceMesh ->
        val title: String = "${scannedFaceMesh.faceMesh.boundingBox.width()}x${scannedFaceMesh.faceMesh.boundingBox.height()}"
        val subtitle: String = "${scannedFaceMesh.faceMesh.allPoints.size} points"

        ResultCardModel(
            title = title,
            subtitle = subtitle,
            imageUri = scannedFaceMesh.imageUri
        )
    }
}