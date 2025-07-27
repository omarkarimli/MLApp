package com.omarkarimli.mlapp.domain.repository

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel

interface ImageLabelingRepository {
    suspend fun scanLive(imageProxy: ImageProxy): Result<List<ImageLabel>>
    suspend fun scanStaticImage(inputImage: InputImage): Result<List<ImageLabel>>
}