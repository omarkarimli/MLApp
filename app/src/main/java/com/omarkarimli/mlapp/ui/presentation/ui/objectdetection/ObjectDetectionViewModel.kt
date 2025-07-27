package com.omarkarimli.mlapp.ui.presentation.ui.objectdetection

import android.Manifest
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.ScannedObject
import com.omarkarimli.mlapp.domain.repository.ObjectDetectionRepository
import com.omarkarimli.mlapp.domain.repository.PermissionRepository
import com.omarkarimli.mlapp.ui.presentation.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ObjectDetectionViewModel @Inject constructor(
    // Injects the PermissionRepository to manage camera and storage permissions.
    val permissionRepository: PermissionRepository,
    // Injects the BarcodeScanningRepository to perform barcode scanning operations.
    private val objectDetectionRepository: ObjectDetectionRepository
) : ViewModel() {

    // MutableStateFlow to hold the current UI state (Idle, Loading, Error, PermissionAction).
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    // Exposed StateFlow for observing UI state changes.
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _objectResults = MutableStateFlow<List<ScannedObject>>(emptyList())
    // Exposed StateFlow for observing scanned barcode results.
    val objectResults: StateFlow<List<ScannedObject>> = _objectResults.asStateFlow()

    // MutableStateFlow to control the active camera (front or back).
    private val _cameraSelector = MutableStateFlow(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA)
    // Exposed StateFlow for observing camera selection changes.
    val cameraSelector: StateFlow<androidx.camera.core.CameraSelector> = _cameraSelector.asStateFlow()

    // StateFlows to observe the status of camera and storage permissions from the repository.
    val hasCameraPermission: StateFlow<Boolean> = permissionRepository.cameraPermissionState
    val hasStoragePermission: StateFlow<Boolean> = permissionRepository.storagePermissionState

    init {
        permissionRepository.notifyPermissionChanged(Manifest.permission.CAMERA)
        permissionRepository.notifyPermissionChanged(permissionRepository.getStoragePermission())
    }

    fun analyzeLiveObject(imageProxy: ImageProxy) {
        // Check for camera permission. If not granted, update UI state and close imageProxy.
        if (!hasCameraPermission.value) {
            _uiState.value = UiState.PermissionAction(Manifest.permission.CAMERA)
            imageProxy.close() // Crucial to close ImageProxy if not processed
            return
        }

        // Launch a coroutine in the viewModelScope to perform asynchronous barcode scanning.
        viewModelScope.launch {
            // Call the repository to scan the live barcode.
            val result = objectDetectionRepository.scanLive(imageProxy)
            result.onSuccess { detectedObjects ->
//                // Convert ML Kit Barcode objects to custom ScannedBarcode domain models.
//                val newDetectedObjects = detectedObjects.map { detectedObject ->
//                    ScannedObject(detectedObject = detectedObject, imageUri = null) // imageUri is null for live scan
//                }
//                // Update the barcode results, ensuring no duplicates based on rawValue.
//                val updatedList = (_objectResults.value + newDetectedObjects).distinctBy { it.detectedObject.trackingId }
//                _objectResults.value = updatedList


                val currentObjects = _objectResults.value.toMutableList()
                val updatedObjects = mutableListOf<ScannedObject>()

                detectedObjects.forEach { detectedObject ->
                    // Check if an object with this trackingId already exists
                    val existingScannedObject = currentObjects.find {
                        it.detectedObject.trackingId == detectedObject.trackingId
                    }

                    if (existingScannedObject != null) {
                        // If exists, update its DetectedObject (e.g., bounding box, labels might change)
                        // Remove from currentObjects to mark it as processed
                        currentObjects.remove(existingScannedObject)
                        updatedObjects.add(ScannedObject(detectedObject = detectedObject, imageUri = null))
                    } else {
                        // If new, add it
                        updatedObjects.add(ScannedObject(detectedObject = detectedObject, imageUri = null))
                    }
                }
                _objectResults.value = updatedObjects
            }.onFailure { e ->
                // If scanning fails, update the UI state to an error state.
                _uiState.value = UiState.Error("Live scanning failed: ${e.message}")
            }
            // The imageProxy is closed in the repository's finally block, so no need to close here again.
        }
    }

    fun analyzeStaticImageForObjects(inputImage: InputImage, imageUri: Uri?) {
        _uiState.value = UiState.Loading
        // Launch a coroutine in the viewModelScope for asynchronous static image scanning.
        viewModelScope.launch {
            // Call the repository to scan the static image.
            val result = objectDetectionRepository.scanStaticImage(inputImage)
            result.onSuccess { detectedObjects ->
                // Convert ML Kit Barcode objects to custom ScannedBarcode domain models,
                // including the imageUri for static scans.
                val newObjects = detectedObjects.map { detectedObject ->
                    ScannedObject(detectedObject = detectedObject, imageUri = imageUri)
                }
                // Update the barcode results, ensuring no duplicates based on rawValue.
                val updatedList = (_objectResults.value + newObjects).distinctBy { it.detectedObject }
                _objectResults.value = updatedList

                // Reset UI state to Idle after successful scanning.
                _uiState.value = UiState.Idle
            }.onFailure { e ->
                _uiState.value = UiState.Error(e.message.toString())
            }
        }
    }

    fun onFlipCamera() {
        _cameraSelector.value = if (_cameraSelector.value == androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA) {
            androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}
