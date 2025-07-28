package com.omarkarimli.mlapp.domain.repository

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh

interface FaceMeshDetectionRepository {
    suspend fun scanLive(imageProxy: ImageProxy): Result<List<FaceMesh>>
    suspend fun scanStaticImage(inputImage: InputImage): Result<List<FaceMesh>>
}