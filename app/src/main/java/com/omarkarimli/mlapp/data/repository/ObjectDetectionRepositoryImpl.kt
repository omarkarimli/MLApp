package com.omarkarimli.mlapp.data.repository

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.omarkarimli.mlapp.domain.repository.ObjectDetectionRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
class ObjectDetectionRepositoryImpl @Inject constructor(
    private val objectDetector: ObjectDetector,
) : ObjectDetectionRepository {

    override suspend fun scanLive(imageProxy: ImageProxy): Result<List<DetectedObject>> {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return Result.failure(IllegalStateException("Media image is null"))
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        return try {
            val detectedObjects = objectDetector.process(image).await()
            Result.success(detectedObjects)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            imageProxy.close()
        }
    }

    override suspend fun scanStaticImage(inputImage: InputImage): Result<List<DetectedObject>> {
        return try {
            val detectedObjects = objectDetector.process(inputImage).await()

            if (detectedObjects.isEmpty()) {
                return Result.failure(RuntimeException("No objects found"))
            } else {
                Result.success(detectedObjects)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}