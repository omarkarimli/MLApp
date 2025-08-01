package com.omarkarimli.mlapp.data.repository

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.omarkarimli.mlapp.domain.repository.ImageLabelingRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
class ImageLabelingRepositoryImpl @Inject constructor(
    private val labeler: ImageLabeler,
) : ImageLabelingRepository {

    override suspend fun scanLive(imageProxy: ImageProxy): Result<List<ImageLabel>> {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return Result.failure(IllegalStateException("Media image is null"))
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        return try {
            val labels = labeler.process(image).await()
            Result.success(labels)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            imageProxy.close()
        }
    }

    override suspend fun scanStaticImage(inputImage: InputImage): Result<List<ImageLabel>> {
        return try {
            val labels = labeler.process(inputImage).await()

            if (labels.isEmpty()) {
                return Result.failure(RuntimeException("No labels found"))
            } else {
                Result.success(labels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}