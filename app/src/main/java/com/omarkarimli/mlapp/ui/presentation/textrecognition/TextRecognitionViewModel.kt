package com.omarkarimli.mlapp.ui.presentation.textrecognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
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

// Data class for recognized text (can be in its own file)
data class RecognizedText(
    val text: String,
    val imageUri: Uri? = null // Nullable for live scans
)

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
        uri?.let {
            viewModelScope.launch {
                analyzeImageForText(context, it) { newResults ->
                    _textResults.update { currentResults ->
                        // Remove any old results from picked images if they existed, or from live scan
                        val updatedList = currentResults.filter { currentInstance -> currentInstance.imageUri == null }.toMutableList()
                        newResults.forEach { newText ->
                            // Only add if it's genuinely new to prevent duplicates when picking same image
                            if (updatedList.none { prevText -> prevText.text == newText.text && prevText.imageUri == newText.imageUri }) {
                                updatedList.add(newText)
                            }
                        }
                        updatedList
                    }
                    if (newResults.isEmpty()) {
                        launch {
                            _toastMessage.emit("No text found in the selected image.")
                        }
                    }
                }
            }
        }
    }

    fun onLiveTextDetected(recognizedText: String) {
        if (recognizedText.isNotEmpty()) {
            _textResults.update { currentResults ->
                // Check if this live recognized text already exists to avoid duplicates
                if (currentResults.none { it.text == recognizedText && it.imageUri == null }) {
                    // Filter out previous live scan results and add the new one
                    val filteredList = currentResults.filter { it.imageUri != null }.toMutableList()
                    filteredList.add(RecognizedText(recognizedText, null))
                    filteredList
                } else {
                    currentResults // No change if already present
                }
            }
        }
    }

    fun onFlipCamera() {
        _cameraSelector.update { currentSelector ->
            if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                Log.e("TextRecognitionViewModel", "Switching to front camera")
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                Log.e("TextRecognitionViewModel", "Switching to back camera")
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            _toastMessage.emit("Save functionality not implemented yet.")
        }
    }

    private suspend fun analyzeImageForText(
        context: Context,
        imageUri: Uri,
        onResult: (List<RecognizedText>) -> Unit
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
                    Log.e("TextRecognizer", "Error decoding image: ${e.message}", e)
                    null
                }

                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val recognizedText = visionText.text
                            val results = if (recognizedText.isNotEmpty()) {
                                listOf(RecognizedText(recognizedText, imageUri))
                            } else {
                                emptyList()
                            }
                            onResult(results)
                        }
                        .addOnFailureListener { e ->
                            Log.e("TextRecognizer", "Image text recognition failed: ${e.message}", e)
                            viewModelScope.launch {
                                _toastMessage.emit("Error analyzing image: ${e.message}")
                            }
                            onResult(emptyList())
                        }
                } else {
                    viewModelScope.launch {
                        _toastMessage.emit("Failed to decode image from gallery.")
                    }
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                Log.e("TextRecognizer", "Unexpected error: ${e.message}", e)
                viewModelScope.launch {
                    _toastMessage.emit("Unexpected error: ${e.message}")
                }
                onResult(emptyList())
            }
        }
    }
}