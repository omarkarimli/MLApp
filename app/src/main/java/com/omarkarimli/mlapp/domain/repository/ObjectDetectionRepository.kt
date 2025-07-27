package com.omarkarimli.mlapp.domain.repository

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject

interface ObjectDetectionRepository {
    suspend fun scanLive(imageProxy: ImageProxy): Result<List<DetectedObject>>
    suspend fun scanStaticImage(inputImage: InputImage): Result<List<DetectedObject>>
}