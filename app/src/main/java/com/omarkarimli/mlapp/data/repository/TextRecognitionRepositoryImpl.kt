package com.omarkarimli.mlapp.data.repository

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.omarkarimli.mlapp.domain.repository.TextRecognitionRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
class TextRecognitionRepositoryImpl @Inject constructor(
    private val recognizer: TextRecognizer
) : TextRecognitionRepository {

    override suspend fun scanLive(imageProxy: ImageProxy): Result<String> {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return Result.failure(IllegalStateException("Media image is null"))
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        return try {
            val recognizedText = recognizer.process(image).await().text

            if (recognizedText.trim().isNotEmpty()) {
                Result.success(recognizedText)
            } else {
                Result.failure(RuntimeException(""))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            imageProxy.close()
        }
    }

    override suspend fun scanStaticImage(inputImage: InputImage): Result<String> {
        return try {
            val recognizedText = recognizer.process(inputImage).await().text

            if (recognizedText.trim().isNotEmpty()) {
                Result.success(recognizedText)
            } else {
                Result.failure(RuntimeException("No text found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
