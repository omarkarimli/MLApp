package com.omarkarimli.mlapp.domain.repository

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

interface TextRecognitionRepository {
    suspend fun scanLive(imageProxy: ImageProxy): Result<String>
    suspend fun scanStaticImage(inputImage: InputImage): Result<String>
}