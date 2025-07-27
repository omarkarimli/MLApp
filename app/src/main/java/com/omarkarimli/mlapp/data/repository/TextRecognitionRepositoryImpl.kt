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
        // Ensure the media image is available from the ImageProxy
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close() // Close the ImageProxy if mediaImage is null
            return Result.failure(IllegalStateException("Media image is null"))
        }

        // Create an InputImage from the media image and its rotation degrees
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        return try {
            // Process the image with the TextRecognizer and await the result
            val recognizedText = recognizer.process(image).await().text

            if (recognizedText.trim().isNotEmpty()) {
                Result.success(recognizedText)
            } else {
                Result.failure(RuntimeException(""))
            }
        } catch (e: Exception) {
            // Return a failure Result if any exception occurs during processing
            Result.failure(e)
        } finally {
            // Ensure the ImageProxy is always closed to release resources
            imageProxy.close()
        }
    }

    override suspend fun scanStaticImage(inputImage: InputImage): Result<String> {
        return try {
            // Process the static InputImage with the TextRecognizer and await the result
            val recognizedText = recognizer.process(inputImage).await().text

            // You might want to add a check for empty text if it's considered a failure case
            if (recognizedText.trim().isNotEmpty()) {
                Result.success(recognizedText)
            } else {
                Result.failure(RuntimeException("No text found"))
            }
        } catch (e: Exception) {
            // Return a failure Result if any exception occurs during processing
            Result.failure(e)
        }
    }
}
