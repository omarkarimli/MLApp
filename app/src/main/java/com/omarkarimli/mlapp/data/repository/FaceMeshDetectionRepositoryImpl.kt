package com.omarkarimli.mlapp.data.repository

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.omarkarimli.mlapp.domain.repository.FaceMeshDetectionRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
class FaceMeshDetectionRepositoryImpl @Inject constructor(
    private val faceMeshDetector: FaceMeshDetector,
) : FaceMeshDetectionRepository {

    override suspend fun scanLive(imageProxy: ImageProxy): Result<List<FaceMesh>> {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return Result.failure(IllegalStateException("Media image is null"))
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        return try {
            val detectedFaceMeshes = faceMeshDetector.process(image).await() // Use .await() here
            Result.success(detectedFaceMeshes)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            imageProxy.close() // Ensure imageProxy is closed
        }
    }

    override suspend fun scanStaticImage(inputImage: InputImage): Result<List<FaceMesh>> {
        return try {
            val detectedFaceMeshes = faceMeshDetector.process(inputImage).await() // Use .await() here

            if (detectedFaceMeshes.isEmpty()) {
                Log.d("FaceMeshDetectionRepo", "No facemesh found")
                return Result.failure(RuntimeException("No facemesh found"))
            } else {
                Log.d("FaceMeshDetectionRepo", "Facemesh found: ${detectedFaceMeshes.size}")
                detectedFaceMeshes.forEach {
                    Log.d("FaceMeshDetectionRepo", "${it.boundingBox.width()}x${it.boundingBox.height()}")
                }

                Result.success(detectedFaceMeshes)
            }
        } catch (e: Exception) {
            Log.e("FaceMeshDetectionRepo", "Error scanning static image: ${e.message}")
            Result.failure(e)
        }
    }
}