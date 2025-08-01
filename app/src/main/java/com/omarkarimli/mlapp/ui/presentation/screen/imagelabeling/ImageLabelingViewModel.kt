package com.omarkarimli.mlapp.ui.presentation.screen.imagelabeling

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
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import com.omarkarimli.mlapp.utils.toResultCards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageLabelingViewModel @Inject constructor(
    val permissionRepository: PermissionRepository,
    private val imageLabelingRepository: ImageLabelingRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _labelingResults = MutableStateFlow<List<ImageLabelResult>>(emptyList())
    val labelingResults: StateFlow<List<ImageLabelResult>> = _labelingResults.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    val hasCameraPermission: StateFlow<Boolean> = permissionRepository.cameraPermissionState
    val hasStoragePermission: StateFlow<Boolean> = permissionRepository.storagePermissionState

    init {
        permissionRepository.notifyPermissionChanged(Manifest.permission.CAMERA)
        permissionRepository.notifyPermissionChanged(permissionRepository.getStoragePermission())

        if (hasCameraPermission.value) {
            toggleCameraActive()
        }
    }

    fun analyzeLiveLabel(imageProxy: ImageProxy) {
        if (!hasCameraPermission.value) {
            _uiState.value = UiState.PermissionAction(Manifest.permission.CAMERA)
            imageProxy.close()
            return
        }

        viewModelScope.launch {
            val result = imageLabelingRepository.scanLive(imageProxy)
            result.onSuccess { labels ->
                val newLabels = labels.map { label ->
                    ImageLabelResult(label = label, imageUri = null)
                }
                val updatedList = (_labelingResults.value + newLabels)
                _labelingResults.value = updatedList
            }.onFailure { e ->
                _uiState.value = UiState.Error("Live scanning failed: ${e.message}")
            }
        }
    }

    fun analyzeStaticImageForLabels(inputImage: InputImage, imageUri: Uri?) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = imageLabelingRepository.scanStaticImage(inputImage)
            result.onSuccess { labels ->
                val newLabels = labels.map { label ->
                    ImageLabelResult(label = label, imageUri = imageUri)
                }
                val updatedList = (_labelingResults.value + newLabels).distinctBy { it.imageUri }
                _labelingResults.value = updatedList

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

    fun toggleCameraActive() {
        if (hasCameraPermission.value) {
            _isCameraActive.value = !_isCameraActive.value
        } else {
            _uiState.value = UiState.Error("Camera permission is required to toggle camera active state.")
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    fun saveCurrentResults() {
        viewModelScope.launch {
            val resultCardsToSave = _labelingResults.value.toResultCards()
            if (resultCardsToSave.isNotEmpty()) {
                resultCardsToSave.forEach { resultCard ->
                    roomRepository.saveResultCard(resultCard)
                }
                _uiState.value = UiState.Idle
            } else {
                _uiState.value = UiState.Error("Nothing to save.")
            }
        }
    }
}