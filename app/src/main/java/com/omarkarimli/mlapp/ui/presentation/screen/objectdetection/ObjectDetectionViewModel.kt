package com.omarkarimli.mlapp.ui.presentation.screen.objectdetection

import android.Manifest
import android.net.Uri
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.ScannedObject
import com.omarkarimli.mlapp.domain.repository.ObjectDetectionRepository
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
class ObjectDetectionViewModel @Inject constructor(
    val permissionRepository: PermissionRepository,
    private val objectDetectionRepository: ObjectDetectionRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _objectResults = MutableStateFlow<List<ScannedObject>>(emptyList())
    val objectResults: StateFlow<List<ScannedObject>> = _objectResults.asStateFlow()

    private val _imageSize = MutableStateFlow(Size(1,1))
    val imageSize: StateFlow<Size> = _imageSize.asStateFlow()

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

    fun analyzeLiveObject(imageProxy: ImageProxy) {
        if (!hasCameraPermission.value) {
            _uiState.value = UiState.PermissionAction(Manifest.permission.CAMERA)
            imageProxy.close()
            return
        }

        viewModelScope.launch {
            val result = objectDetectionRepository.scanLive(imageProxy)
            result.onSuccess { detectedObjects ->
                val previousStaticObjects = _objectResults.value.filter { it.imageUri != null }
                _objectResults.value = (previousStaticObjects + detectedObjects.map { detectedObject ->
                    ScannedObject(detectedObject = detectedObject, imageUri = null)
                }).distinctBy { it.detectedObject }

                _imageSize.value = Size(imageProxy.width, imageProxy.height)
            }.onFailure { e ->
                _uiState.value = UiState.Error("Live scanning failed: ${e.message}")
            }
        }
    }

    fun analyzeStaticImageForObjects(inputImage: InputImage, imageUri: Uri?) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = objectDetectionRepository.scanStaticImage(inputImage)
            result.onSuccess { detectedObjects ->
                _objectResults.value = _objectResults.value + detectedObjects.map { detectedObject ->
                    ScannedObject(detectedObject = detectedObject, imageUri = imageUri)
                }.distinctBy { it.detectedObject.trackingId }

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
            val resultCardsToSave = _objectResults.value.toResultCards()
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