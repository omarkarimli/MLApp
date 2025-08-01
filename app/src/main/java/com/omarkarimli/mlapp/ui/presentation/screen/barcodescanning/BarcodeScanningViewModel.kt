package com.omarkarimli.mlapp.ui.presentation.screen.barcodescanning

import android.Manifest
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.ScannedBarcode
import com.omarkarimli.mlapp.domain.repository.BarcodeScanningRepository
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
class BarcodeScanningViewModel @Inject constructor(
    val permissionRepository: PermissionRepository,
    private val barcodeRepository: BarcodeScanningRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _barcodeResults = MutableStateFlow<List<ScannedBarcode>>(emptyList())
    val barcodeResults: StateFlow<List<ScannedBarcode>> = _barcodeResults.asStateFlow()

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

    fun analyzeLiveBarcode(imageProxy: ImageProxy) {
        if (!hasCameraPermission.value) {
            _uiState.value = UiState.PermissionAction(Manifest.permission.CAMERA)
            imageProxy.close()
            return
        }

        viewModelScope.launch {
            val result = barcodeRepository.scanLive(imageProxy)
            result.onSuccess { barcodes ->
                val newScannedBarcodes = barcodes.map { barcode ->
                    ScannedBarcode(barcode = barcode, imageUri = null)
                }
                val updatedList = (_barcodeResults.value + newScannedBarcodes).distinctBy { it.barcode.rawValue }
                _barcodeResults.value = updatedList
            }.onFailure { e ->
                _uiState.value = UiState.Error("Live scanning failed: ${e.message}")
            }
        }
    }

    fun analyzeStaticImageForBarcodes(inputImage: InputImage, imageUri: Uri?) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = barcodeRepository.scanStaticImage(inputImage)
            result.onSuccess { barcodes ->
                val newScannedBarcodes = barcodes.map { barcode ->
                    ScannedBarcode(barcode = barcode, imageUri = imageUri)
                }
                val updatedList = (_barcodeResults.value + newScannedBarcodes).distinctBy { it.barcode.rawValue }
                _barcodeResults.value = updatedList

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
            val resultCardsToSave = _barcodeResults.value.toResultCards()
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