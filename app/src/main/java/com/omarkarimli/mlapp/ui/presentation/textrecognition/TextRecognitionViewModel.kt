package com.omarkarimli.mlapp.ui.presentation.textrecognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecognizedText(
    val text: String,
    val imageUri: Uri? = null // Nullable: null for live scans, non-null for picked images
)

@OptIn(ExperimentalGetImage::class)
class TextRecognitionViewModel : ViewModel() {

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission = _hasCameraPermission.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission = _hasStoragePermission.asStateFlow()

    private val _textResults = MutableStateFlow<List<RecognizedText>>(emptyList())
    val textResults = _textResults.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // ML Kit Text Recognizer instance
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun updateCameraPermission(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
        if (!isGranted) {
            viewModelScope.launch {
                _toastMessage.emit("Camera permission is required for live scanning.")
            }
        }
    }

    fun updateStoragePermission(isGranted: Boolean) {
        _hasStoragePermission.value = isGranted
        if (!isGranted) {
            viewModelScope.launch {
                _toastMessage.emit("Storage permission is required to pick photos.")
            }
        }
    }

    fun onImagePicked(context: Context, uri: Uri?) {
        uri?.let { pickedUri ->
            viewModelScope.launch {
                analyzeImageForText(context, pickedUri) { newPickedText ->
                    _textResults.update { currentResults ->
                        val updatedList = currentResults.filter { it.imageUri == null }.toMutableList()

                        if (newPickedText != null) {
                            updatedList.add(newPickedText)
                        } else {
                            launch {
                                _toastMessage.emit("No text found in the selected image.")
                            }
                        }
                        updatedList
                    }
                }
            }
        } ?: viewModelScope.launch {
            _toastMessage.emit("No image selected.")
        }
    }

    fun onLiveTextDetected(recognizedText: String) {
        if (recognizedText.isNotEmpty()) {
            _textResults.update { currentResults ->
                // Check if the recognizedText already exists in the list
                val isAlreadyPresent = currentResults.any { it.text == recognizedText }

                if (!isAlreadyPresent) {
                    // If it's not already present, add the new live text result
                    val updatedList = currentResults.toMutableList()
                    updatedList.add(RecognizedText(recognizedText, null))
                    updatedList // Return the updated list
                } else {
                    currentResults // Return the current list unchanged
                }
            }
        }
    }

    fun onFlipCamera() {
        _cameraSelector.update { currentSelector ->
            if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                Log.d("TextRecognitionViewModel", "Switching to front camera")
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                Log.d("TextRecognitionViewModel", "Switching to back camera")
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            _toastMessage.emit("Save functionality not implemented yet.")
        }
    }

    fun createImageAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = visionText.text
                        // Pass the detected text back to the ViewModel's handler
                        onLiveTextDetected(recognizedText)
                    }
                    .addOnFailureListener { e ->
                        Log.e("TextRecognitionViewModel", "Live text recognition failed: ${e.message}", e)
                        // Optionally, emit a toast for persistent errors, but usually not for live analysis
                    }
                    .addOnCompleteListener {
                        imageProxy.close() // Important to close the image proxy
                    }
            } else {
                imageProxy.close() // Always close the image proxy
            }
        }
    }

    private suspend fun analyzeImageForText(
        context: Context,
        imageUri: Uri,
        onResult: (RecognizedText?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val bitmap: Bitmap? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                    }
                } catch (e: Exception) {
                    Log.e("TextRecognizer", "Error decoding image URI: ${e.message}", e)
                    null
                }

                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)

                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val recognizedText = visionText.text
                            if (recognizedText.isNotEmpty()) {
                                onResult(RecognizedText(recognizedText, imageUri))
                            } else {
                                onResult(null)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("TextRecognizer", "Image text recognition failed: ${e.message}", e)
                            viewModelScope.launch {
                                _toastMessage.emit("Error analyzing image: ${e.message}")
                            }
                            onResult(null)
                        }
                } else {
                    viewModelScope.launch {
                        _toastMessage.emit("Failed to decode image from gallery.")
                    }
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("TextRecognizer", "Unexpected error during image analysis: ${e.message}", e)
                viewModelScope.launch {
                    _toastMessage.emit("Unexpected error: ${e.message}")
                }
                onResult(null)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Release the ML Kit Text Recognizer when the ViewModel is cleared
        textRecognizer.close()
    }
}