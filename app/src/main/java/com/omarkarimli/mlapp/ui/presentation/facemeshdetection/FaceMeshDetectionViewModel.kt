package com.omarkarimli.mlapp.ui.presentation.facemeshdetection

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
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannedFaceMesh(
    val faceMesh: FaceMesh,
    val imageUri: Uri? = null // Nullable for live scans
)

class FaceMeshDetectionViewModel : ViewModel() {

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission = _hasCameraPermission.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission = _hasStoragePermission.asStateFlow()

    private val _faceMeshResults = mutableStateListOf<ScannedFaceMesh>()
    val faceMeshResults: List<ScannedFaceMesh> = _faceMeshResults

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    private val _detectedFaceMeshesForOverlay = MutableStateFlow<List<FaceMesh>>(emptyList())
    val detectedFaceMeshesForOverlay = _detectedFaceMeshesForOverlay.asStateFlow()

    private val _imageSizeForOverlay = MutableStateFlow(Size(1, 1))
    val imageSizeForOverlay = _imageSizeForOverlay.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

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
                analyzeImageForFaceMeshes(context, it) { results ->
                    // Clear existing live scan results when a new image is picked
                    _faceMeshResults.removeAll { scannedFaceMesh -> scannedFaceMesh.imageUri == null }

                    results.forEach { newScannedFaceMesh ->
                        // Simple check to avoid duplicates for now, based on bounding box
                        if (_faceMeshResults.none { prevScannedFaceMesh ->
                                prevScannedFaceMesh.faceMesh.boundingBox == newScannedFaceMesh.faceMesh.boundingBox && prevScannedFaceMesh.imageUri == newScannedFaceMesh.imageUri
                            }) {
                            _faceMeshResults.add(newScannedFaceMesh)
                        }
                    }
                }
            }
        }
    }

    fun onFaceMeshesDetected(faceMeshes: List<FaceMesh>, imageWidth: Int, imageHeight: Int) {
        _detectedFaceMeshesForOverlay.value = faceMeshes
        _imageSizeForOverlay.value = Size(imageWidth, imageHeight)

        // Filter out live scan results, then add new ones
        val currentLiveFaceMeshes = _faceMeshResults.filter { it.imageUri == null }.map { it.faceMesh.boundingBox }.toSet()
        val newLiveFaceMeshes = faceMeshes.map { it.boundingBox }.toSet()

        if (newLiveFaceMeshes != currentLiveFaceMeshes) {
            _faceMeshResults.removeAll { it.imageUri == null } // Remove old live scan results
            faceMeshes.forEach { newFaceMesh ->
                // Only add if it's genuinely new to the live scan list
                if (_faceMeshResults.none { existingFaceMesh ->
                        existingFaceMesh.imageUri == null &&
                                existingFaceMesh.faceMesh.boundingBox == newFaceMesh.boundingBox
                    }) {
                    _faceMeshResults.add(ScannedFaceMesh(newFaceMesh, null))
                }
            }
        }
    }

    fun onFlipCamera() {
        _cameraSelector.update { currentSelector ->
            if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                Log.e("FaceMeshDetectionScreen", "Switching to front camera")
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                Log.e("FaceMeshDetectionScreen", "Switching to back camera")
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            _toastMessage.emit("Save functionality not implemented yet.")
        }
    }

    private suspend fun analyzeImageForFaceMeshes(context: Context, imageUri: Uri, onResult: (List<ScannedFaceMesh>) -> Unit) {
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
                    Log.e("FaceMeshDetector", "Error decoding image: ${e.message}", e)
                    null
                }

                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)

                    val options = FaceMeshDetectorOptions.Builder().build()
                    val faceMeshDetector = FaceMeshDetection.getClient(options)

                    faceMeshDetector.process(image)
                        .addOnSuccessListener { faceMeshes ->
                            val scannedFaceMeshes = faceMeshes.map { ScannedFaceMesh(it, imageUri) }
                            onResult(scannedFaceMeshes)
                            if (faceMeshes.isEmpty()) {
                                viewModelScope.launch {
                                    _toastMessage.emit("No face meshes found in the selected image.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FaceMeshDetector", "Image face mesh detection failed: ${e.message}", e)
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
                Log.e("FaceMeshDetector", "Unexpected error: ${e.message}", e)
                viewModelScope.launch {
                    _toastMessage.emit("Unexpected error: ${e.message}")
                }
            }
        }
    }
}
