package com.omarkarimli.mlapp.ui.presentation.ui.imagelabeling

import android.Manifest
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.ImageLabelResult
import com.omarkarimli.mlapp.domain.repository.ImageLabelingRepository
import com.omarkarimli.mlapp.domain.repository.PermissionRepository
import com.omarkarimli.mlapp.ui.presentation.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageLabelingViewModel @Inject constructor(
    // Injects the PermissionRepository to manage camera and storage permissions.
    val permissionRepository: PermissionRepository,
    private val imageLabelingRepository: ImageLabelingRepository
) : ViewModel() {

    // MutableStateFlow to hold the current UI state (Idle, Loading, Error, PermissionAction).
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    // Exposed StateFlow for observing UI state changes.
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // MutableStateFlow to hold the list of scanned barcodes.
    private val _labelingResults = MutableStateFlow<List<ImageLabelResult>>(emptyList())
    val labelingResults: StateFlow<List<ImageLabelResult>> = _labelingResults.asStateFlow()

    // MutableStateFlow to control the active camera (front or back).
    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    // Exposed StateFlow for observing camera selection changes.
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    // StateFlows to observe the status of camera and storage permissions from the repository.
    val hasCameraPermission: StateFlow<Boolean> = permissionRepository.cameraPermissionState
    val hasStoragePermission: StateFlow<Boolean> = permissionRepository.storagePermissionState

    init {
        permissionRepository.notifyPermissionChanged(Manifest.permission.CAMERA)
        permissionRepository.notifyPermissionChanged(permissionRepository.getStoragePermission())
    }

    fun analyzeLiveLabel(imageProxy: ImageProxy) {
        // Check for camera permission. If not granted, update UI state and close imageProxy.
        if (!hasCameraPermission.value) {
            _uiState.value = UiState.PermissionAction(Manifest.permission.CAMERA)
            imageProxy.close() // Crucial to close ImageProxy if not processed
            return
        }

        // Launch a coroutine in the viewModelScope to perform asynchronous barcode scanning.
        viewModelScope.launch {
            // Call the repository to scan the live barcode.
            val result = imageLabelingRepository.scanLive(imageProxy)
            result.onSuccess { labels ->
                // Convert ML Kit Barcode objects to custom ScannedBarcode domain models.
                val newLabels = labels.map { label ->
                    ImageLabelResult(label = label, imageUri = null) // imageUri is null for live scan
                }
                // Update the barcode results, ensuring no duplicates based on rawValue.
                val updatedList = (_labelingResults.value + newLabels)
                _labelingResults.value = updatedList
            }.onFailure { e ->
                // If scanning fails, update the UI state to an error state.
                _uiState.value = UiState.Error("Live scanning failed: ${e.message}")
            }
            // The imageProxy is closed in the repository's finally block, so no need to close here again.
        }
    }

    fun analyzeStaticImageForLabels(inputImage: InputImage, imageUri: Uri?) {
        _uiState.value = UiState.Loading
        // Launch a coroutine in the viewModelScope for asynchronous static image scanning.
        viewModelScope.launch {
            // Call the repository to scan the static image.
            val result = imageLabelingRepository.scanStaticImage(inputImage)
            result.onSuccess { labels ->
                // Convert ML Kit Barcode objects to custom ScannedBarcode domain models,
                // including the imageUri for static scans.
                val newLabels = labels.map { label ->
                    ImageLabelResult(label = label, imageUri = imageUri)
                }
                // Update the barcode results, ensuring no duplicates based on rawValue.
                val updatedList = (_labelingResults.value + newLabels).distinctBy { it.imageUri }
                _labelingResults.value = updatedList

                // Reset UI state to Idle after successful scanning.
                _uiState.value = UiState.Idle
            }.onFailure { e ->
                _uiState.value = UiState.Error(e.message.toString())
            }
        }
    }

    fun onFlipCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}
