package com.omarkarimli.mlapp.ui.presentation.ui.facemeshdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.omarkarimli.mlapp.domain.models.ScannedFaceMesh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FaceMeshDetectionViewModel : ViewModel() {

    // ML Kit Face Mesh Detector instance for live analysis
    private val faceMeshDetectorOptions = FaceMeshDetectorOptions.Builder().build()
    private val liveFaceMeshDetector = FaceMeshDetection.getClient(faceMeshDetectorOptions)

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission = _hasCameraPermission.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission = _hasStoragePermission.asStateFlow()

    private val _faceMeshResults = MutableStateFlow<MutableList<ScannedFaceMesh>>(mutableListOf()) // Changed to MutableStateFlow<MutableList> for consistency
    val faceMeshResults = _faceMeshResults.asStateFlow() // Expose as StateFlow<List>

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    private val _detectedFaceMeshesForOverlay = MutableStateFlow<List<FaceMesh>>(emptyList())
    val detectedFaceMeshesForOverlay = _detectedFaceMeshesForOverlay.asStateFlow()

    private val _imageSizeForOverlay = MutableStateFlow(Size(1, 1))
    val imageSizeForOverlay = _imageSizeForOverlay.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // Flag to indicate if the bottom sheet should expand
    private val _shouldExpandBottomSheet = MutableStateFlow(false)
    val shouldExpandBottomSheet = _shouldExpandBottomSheet.asStateFlow()

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
                    _faceMeshResults.update { currentList ->
                        // Clear existing live scan results when a new image is picked
                        val filteredList = currentList.filter { scannedFaceMesh -> scannedFaceMesh.imageUri != null }.toMutableList()
                        results.forEach { newScannedFaceMesh ->
                            // Simple check to avoid duplicates for now, based on bounding box
                            if (filteredList.none { prevScannedFaceMesh ->
                                    prevScannedFaceMesh.faceMesh.boundingBox == newScannedFaceMesh.faceMesh.boundingBox && prevScannedFaceMesh.imageUri == newScannedFaceMesh.imageUri
                                }) {
                                filteredList.add(newScannedFaceMesh)
                            }
                        }
                        filteredList
                    }
                    if (results.isNotEmpty()) {
                        _shouldExpandBottomSheet.value = true
                    }
                }
            }
        }
    }

    // This function will be called from the CameraPreview's analyzer
    @OptIn(ExperimentalGetImage::class)
    fun analyzeLiveFaceMesh(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val imageWidth = image.width
            val imageHeight = image.height

            liveFaceMeshDetector.process(image)
                .addOnSuccessListener { faceMeshes ->
                    // Update ViewModel state for overlay
                    _detectedFaceMeshesForOverlay.value = faceMeshes
                    _imageSizeForOverlay.value = Size(imageWidth, imageHeight)

                    // Also update faceMeshResults for the bottom sheet
                    _faceMeshResults.update { currentList ->
                        val updatedList = currentList.filter { it.imageUri != null }.toMutableList() // Keep picked images
                        faceMeshes.forEach { newFaceMesh ->
                            // Add new live face meshes, avoiding duplicates based on bounding box
                            if (updatedList.none { existingFaceMesh ->
                                    existingFaceMesh.imageUri == null &&
                                            existingFaceMesh.faceMesh.boundingBox == newFaceMesh.boundingBox
                                }) {
                                updatedList.add(ScannedFaceMesh(newFaceMesh, null))
                            }
                        }
                        updatedList
                    }

                    if (faceMeshes.isNotEmpty()) {
                        _shouldExpandBottomSheet.value = true
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceMeshDetector", "Live face mesh detection failed: ${e.message}", e)
                    // Clear overlay on failure, but keep the size if previously set for stability
                    _detectedFaceMeshesForOverlay.value = emptyList()
                    // Optionally show a toast for recurring errors, but often too noisy for live feed
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
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

    // Reset the shouldExpandBottomSheet flag after it's been consumed by the UI
    fun resetBottomSheetExpansion() {
        _shouldExpandBottomSheet.value = false
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

                    // Create a new scanner instance for static images to avoid potential conflicts
                    val staticImageFaceMeshDetector = FaceMeshDetection.getClient(faceMeshDetectorOptions)
                    staticImageFaceMeshDetector.process(image)
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
                        .addOnCompleteListener {
                            // Release resources for the static image scanner
                            staticImageFaceMeshDetector.close()
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

    override fun onCleared() {
        super.onCleared()
        liveFaceMeshDetector.close() // Release ML Kit resources for the live scanner
    }
}