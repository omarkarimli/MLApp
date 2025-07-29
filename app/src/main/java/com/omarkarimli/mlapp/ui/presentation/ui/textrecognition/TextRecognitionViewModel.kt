package com.omarkarimli.mlapp.ui.presentation.ui.textrecognition

import android.Manifest
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.RecognizedText
import com.omarkarimli.mlapp.domain.repository.PermissionRepository
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import com.omarkarimli.mlapp.domain.repository.TextRecognitionRepository // Import TextRecognitionRepository
import com.omarkarimli.mlapp.ui.presentation.ui.common.state.UiState
import com.omarkarimli.mlapp.utils.toResultCards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TextRecognitionViewModel @Inject constructor(
    // Injects the PermissionRepository to manage camera and storage permissions.
    val permissionRepository: PermissionRepository,
    // Injects the TextRecognitionRepository to perform text recognition operations.
    private val textRecognitionRepository: TextRecognitionRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    // MutableStateFlow to hold the current UI state (Idle, Loading, Error, PermissionAction).
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    // Exposed StateFlow for observing UI state changes.
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // MutableStateFlow to hold the recognized text result.
    private val _textResults = MutableStateFlow<List<RecognizedText>>(emptyList())
    // Exposed StateFlow for observing scanned barcode results.
    val textResults: StateFlow<List<RecognizedText>> = _textResults.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _cameraSelector = MutableStateFlow(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA)
    // Exposed StateFlow for observing camera selection changes.
    val cameraSelector: StateFlow<androidx.camera.core.CameraSelector> = _cameraSelector.asStateFlow()

    // StateFlows to observe the status of camera and storage permissions from the repository.
    val hasCameraPermission: StateFlow<Boolean> = permissionRepository.cameraPermissionState
    val hasStoragePermission: StateFlow<Boolean> = permissionRepository.storagePermissionState

    init {
        // Initialize permission states when the ViewModel is created.
        permissionRepository.notifyPermissionChanged(Manifest.permission.CAMERA)
        permissionRepository.notifyPermissionChanged(permissionRepository.getStoragePermission())

        if (hasCameraPermission.value) {
            toggleCameraActive()
        }
    }

    fun analyzeLiveText(imageProxy: ImageProxy) {
        // Check for camera permission. If not granted, update UI state and close imageProxy.
        if (!hasCameraPermission.value) {
            _uiState.value = UiState.PermissionAction(Manifest.permission.CAMERA)
            imageProxy.close() // Crucial to close ImageProxy if not processed
            return
        }

        // Launch a coroutine in the viewModelScope to perform asynchronous text recognition.
        viewModelScope.launch {
            // Call the repository to scan the live image for text.
            val result = textRecognitionRepository.scanLive(imageProxy)
            result.onSuccess { recognizedText ->
                // Update the barcode results, ensuring no duplicates based on rawValue.
                val updatedList = (_textResults.value + RecognizedText(recognizedText, null)).distinctBy { it.text }
                _textResults.value = updatedList
            }.onFailure { e ->
                // If scanning fails, update the UI state to an error state.
                if (e.message.toString().trim().isNotEmpty()) _uiState.value = UiState.Error(e.message.toString())
            }
            // The imageProxy is closed in the repository's finally block, so no need to close here again.
        }
    }

    fun analyzeStaticImageForText(inputImage: InputImage, imageUri: Uri?) {
        _uiState.value = UiState.Loading // Set UI state to loading during processing.
        // Launch a coroutine in the viewModelScope for asynchronous static image text recognition.
        viewModelScope.launch {
            // Call the repository to scan the static image for text.
            val result = textRecognitionRepository.scanStaticImage(inputImage)
            result.onSuccess { recognizedText ->
                // Update the recognized text result.
                val updatedList = (_textResults.value + RecognizedText(recognizedText, imageUri)).distinctBy { it.text }
                _textResults.value = updatedList
                // Reset UI state to Idle after successful scanning.
                _uiState.value = UiState.Idle
            }.onFailure { e ->
                // If scanning fails, update the UI state to an error state.
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
            val resultCardsToSave = _textResults.value.toResultCards()
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
