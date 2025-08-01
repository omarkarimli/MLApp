package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.facemesh.FaceMesh

data class ScannedFaceMesh(
    val faceMesh: FaceMesh,
    val imageUri: Uri? = null
)
