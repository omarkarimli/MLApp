package com.omarkarimli.mlapp.ui.presentation.ui.facemeshdetection

import android.Manifest
import android.net.Uri
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.ScannedFaceMesh
import com.omarkarimli.mlapp.domain.repository.FaceMeshDetectionRepository
import com.omarkarimli.mlapp.domain.repository.PermissionRepository
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import com.omarkarimli.mlapp.ui.presentation.ui.common.state.UiState
import com.omarkarimli.mlapp.utils.toResultCards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaceMeshDetectionViewModel @Inject constructor(
    val permissionRepository: PermissionRepository,
    private val faceMeshDetectionRepository: FaceMeshDetectionRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _faceMeshResults = MutableStateFlow<List<ScannedFaceMesh>>(emptyList())
    val faceMeshResults: StateFlow<List<ScannedFaceMesh>> = _faceMeshResults.asStateFlow()

    private val _imageSize = MutableStateFlow(Size(1,1))
    val imageSize: StateFlow<Size> = _imageSize.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _cameraSelector = MutableStateFlow(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA)
    // Exposed StateFlow for observing camera selection changes.
    val cameraSelector: StateFlow<androidx.camera.core.CameraSelector> = _cameraSelector.asStateFlow()

    // StateFlows to observe the status of camera and storage permissions from the repository.
    val hasCameraPermission: StateFlow<Boolean> = permissionRepository.cameraPermissionState
    val hasStoragePermission: StateFlow<Boolean> = permissionRepository.storagePermissionState

    init {
        permissionRepository.notifyPermissionChanged(Manifest.permission.CAMERA)
        permissionRepository.notifyPermissionChanged(permissionRepository.getStoragePermission())

        if (hasCameraPermission.value) {
            toggleCameraActive()
        }
    }

    fun analyzeLiveFaceMesh(imageProxy: ImageProxy) {
        // Check for camera permission. If not granted, update UI state and close imageProxy.
        if (!hasCameraPermission.value) {
            _uiState.value = UiState.PermissionAction(Manifest.permission.CAMERA)
            imageProxy.close() // Crucial to close ImageProxy if not processed
            return
        }

        // Launch a coroutine in the viewModelScope to perform asynchronous barcode scanning.
        viewModelScope.launch {
            val result = faceMeshDetectionRepository.scanLive(imageProxy)
            result.onSuccess { detectedFaceMeshes ->
                // Filter Previous FaceMeshes to include only those with a non-null imageUri
                val previousStaticFaceMeshes = _faceMeshResults.value.filter { it.imageUri != null }
                _faceMeshResults.value = previousStaticFaceMeshes + detectedFaceMeshes.map { detectedFaceMesh ->
                    ScannedFaceMesh(faceMesh = detectedFaceMesh, imageUri = null)
                }

                _imageSize.value = Size(imageProxy.width, imageProxy.height)
            }.onFailure { e ->
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
            val result = faceMeshDetectionRepository.scanStaticImage(inputImage)
            result.onSuccess { detectedFaceMeshes ->
                _faceMeshResults.value = _faceMeshResults.value + detectedFaceMeshes.map { detectedFaceMesh ->
                    ScannedFaceMesh(faceMesh = detectedFaceMesh, imageUri = imageUri)
                }

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

    fun toggleCameraActive() {
        if (hasCameraPermission.value) {
            _isCameraActive.value = !_isCameraActive.value
//            // Clear live analysis results when pausing, keep static ones
//            if (!_isCameraActive.value) {
//                _faceMeshResults.value = _faceMeshResults.value.filter { it.imageUri != null }
//            }
        } else {
            _uiState.value = UiState.Error("Camera permission is required to toggle camera active state.")
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    fun saveCurrentResults() {
        viewModelScope.launch {
            val resultCardsToSave = _faceMeshResults.value.toResultCards()
            if (resultCardsToSave.isNotEmpty()) {
                resultCardsToSave.forEach { resultCard ->
                    roomRepository.saveResultCard(resultCard)
                }
                _uiState.value = UiState.Idle // Indicate successful save
            } else {
                _uiState.value = UiState.Error("Nothing to save.")
            }
        }
    }
}
