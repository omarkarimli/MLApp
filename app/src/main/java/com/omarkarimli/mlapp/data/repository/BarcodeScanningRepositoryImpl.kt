package com.omarkarimli.mlapp.data.repository

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.repository.BarcodeScanningRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
class BarcodeScanningRepositoryImpl @Inject constructor(
    private val scanner: BarcodeScanner,
) : BarcodeScanningRepository {

    override suspend fun scanLive(imageProxy: ImageProxy): Result<List<Barcode>> {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return Result.failure(IllegalStateException("Media image is null"))
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        return try {
            val barcodes = scanner.process(image).await()
            Result.success(barcodes)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            imageProxy.close()
        }
    }

    override suspend fun scanStaticImage(inputImage: InputImage): Result<List<Barcode>> {
        return try {
            val barcodes = scanner.process(inputImage).await()

            if (barcodes.isEmpty()) {
                return Result.failure(RuntimeException("No barcodes found"))
            } else {
                Result.success(barcodes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}