package com.omarkarimli.mlapp.ui.presentation.ui.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.omarkarimli.mlapp.domain.models.ScannedObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ObjectDetectionViewModel : ViewModel() {

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission = _hasCameraPermission.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission = _hasStoragePermission.asStateFlow()

    private val _objectResults = mutableStateListOf<ScannedObject>()
    val objectResults: List<ScannedObject> = _objectResults

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    private val _detectedObjectsForOverlay = MutableStateFlow<List<DetectedObject>>(emptyList())
    val detectedObjectsForOverlay = _detectedObjectsForOverlay.asStateFlow()

    private val _currentImageSize = MutableStateFlow(Size(1, 1))
    val currentImageSize = _currentImageSize.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // Object detector for single image mode (gallery)
    private val singleImageObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    fun updateCameraPermission(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
        if (!isGranted) {
            viewModelScope.launch {
                _toastMessage.emit("Camera permission is required for live detection.")
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
                analyzeImageForObjects(context, it) { results ->
                    // Clear existing live scan results when a new image is picked
                    _objectResults.removeAll { scannedObject -> scannedObject.imageUri == null }

                    results.forEach { newScannedObject ->
                        // Improved duplicate check for picked images based on label and URI
                        val newLabel = newScannedObject.detectedObject.labels.firstOrNull()?.text
                        if (_objectResults.none { prevScannedObject ->
                                prevScannedObject.detectedObject.labels.firstOrNull()?.text == newLabel && prevScannedObject.imageUri == newScannedObject.imageUri
                            }) {
                            _objectResults.add(newScannedObject)
                        }
                    }
                }
            }
        }
    }


    fun onObjectsDetected(objects: List<DetectedObject>, imageWidth: Int, imageHeight: Int) {
        _detectedObjectsForOverlay.value = objects
        _currentImageSize.value = Size(imageWidth, imageHeight)

        // Filter out live scan results, then add new ones
        // We're moving away from the Set comparison for more granular control below.

        val updatedObjectResults = _objectResults.toMutableList() // Create a mutable copy to work with

        // First, remove old live scan results to ensure we're processing the latest frame's objects
        updatedObjectResults.removeAll { it.imageUri == null }

        objects.forEach { newObject ->
            // Create a unique identifier for the new live object based on its label and bounding box
            val newObjectLabel = newObject.labels.firstOrNull()?.text
            val newObjectBoundingBox = newObject.boundingBox

            // Check if a similar live object already exists in the updated list
            val isAlreadyPresent = updatedObjectResults.any { existingObject ->
                existingObject.imageUri == null && // Only consider live scan objects
                        existingObject.detectedObject.labels.firstOrNull()?.text == newObjectLabel &&
                        existingObject.detectedObject.boundingBox == newObjectBoundingBox
            }

            // Only add if it's genuinely new to the live scan list
            if (!isAlreadyPresent) {
                updatedObjectResults.add(ScannedObject(newObject, null))
            }
        }

        // Update the _objectResults with the new, de-duplicated list
        _objectResults.addAll(updatedObjectResults)
    }

    fun onFlipCamera() {
        _cameraSelector.update { currentSelector ->
            if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                Log.e("ObjectDetectionScreen", "Switching to front camera")
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                Log.e("ObjectDetectionScreen", "Switching to back camera")
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            _toastMessage.emit("Save functionality not implemented yet.")
        }
    }

    // Moved analyzeImageForObjects into ViewModel for better encapsulation
    private suspend fun analyzeImageForObjects(context: Context, imageUri: Uri, onResult: (List<ScannedObject>) -> Unit) {
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
                    Log.e("ObjectDetector", "Error decoding image: ${e.message}", e)
                    null
                }

                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)

                    singleImageObjectDetector.process(image) // Use the dedicated single-image detector
                        .addOnSuccessListener { detectedObjects ->
                            val scannedObjects = detectedObjects.map { ScannedObject(it, imageUri) }
                            onResult(scannedObjects)
                            if (detectedObjects.isEmpty()) {
                                viewModelScope.launch {
                                    _toastMessage.emit("No objects found in the selected image.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ObjectDetector", "Image object detection failed: ${e.message}", e)
                            viewModelScope.launch {
                                _toastMessage.emit("Error analyzing image: ${e.message}")
                            }
                        }
                } else {
                    viewModelScope.launch {
                        _toastMessage.emit("Failed to decode image from gallery.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ObjectDetector", "Unexpected error: ${e.message}", e)
                viewModelScope.launch {
                    _toastMessage.emit("Unexpected error: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Close the object detector when the ViewModel is no longer used
        singleImageObjectDetector.close()
    }
}