package com.omarkarimli.mlapp.domain.repository

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

interface BarcodeScanningRepository {
    suspend fun scanLive(imageProxy: ImageProxy): Result<List<Barcode>>
    suspend fun scanStaticImage(inputImage: InputImage): Result<List<Barcode>>
}