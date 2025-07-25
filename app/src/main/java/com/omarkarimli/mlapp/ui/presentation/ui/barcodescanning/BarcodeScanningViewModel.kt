package com.omarkarimli.mlapp.ui.presentation.ui.barcodescanning

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.ScannedBarcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Note: No more MLRepository injection for permission checks, as they are now in the UI.
// If you have other ML-related logic that needs a repository, keep it.
// For this example, I'm removing the MLRepository dependency as it was only for permissions.
class BarcodeScanningViewModel() : ViewModel() {

    // Represents the UI state for barcode scanning
    sealed class BarcodeScanUiState {
        object Idle : BarcodeScanUiState()
        object Loading : BarcodeScanUiState()
        data class Error(val message: String) : BarcodeScanUiState()
    }

    private val _uiState = MutableStateFlow<BarcodeScanUiState>(BarcodeScanUiState.Idle)
    val uiState: StateFlow<BarcodeScanUiState> = _uiState.asStateFlow()

    // Permissions are managed directly in the UI now.
    // _hasCameraPermission and _hasStoragePermission are removed.

    private val _barcodeResults = MutableStateFlow<MutableList<ScannedBarcode>>(mutableListOf())
    val barcodeResults: StateFlow<List<ScannedBarcode>> = _barcodeResults.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    // _shouldExpandBottomSheet is removed. UI will observe barcodeResults.size directly.

    // Barcode scanner instance for live camera analysis
    private val barcodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
    private val liveBarcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

    // setCameraPermission and setStoragePermission are removed as they are no longer needed here.
    // initializePermissions is removed.

    fun onBarcodeDetected(barcodes: List<Barcode>, sourceImageUri: android.net.Uri? = null) {
        val currentResults = _barcodeResults.value
        val newBarcodes = barcodes.filter { newBarcode ->
            currentResults.none { it.barcode.rawValue == newBarcode.rawValue }
        }.map { ScannedBarcode(it, sourceImageUri) }

        if (newBarcodes.isNotEmpty()) {
            _barcodeResults.update { (it + newBarcodes).toMutableList() }
            // _shouldExpandBottomSheet.value = true; -- REMOVED
            _uiState.value = BarcodeScanUiState.Idle // Reset UI state after success
        } else {
            _uiState.value = BarcodeScanUiState.Idle
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun analyzeLiveBarcode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            _uiState.value = BarcodeScanUiState.Loading // Indicate loading for live analysis
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            liveBarcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onBarcodeDetected(barcodes, null) // No specific URI for live camera
                    } else {
                        _uiState.value = BarcodeScanUiState.Idle // No barcodes found, but no error
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BarcodeScannerVM", "Live barcode scanning failed: ${e.message}", e)
                    _uiState.value = BarcodeScanUiState.Error("Failed to scan live barcode: ${e.message}")
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
            _uiState.value = BarcodeScanUiState.Error("Failed to get image from ImageProxy.")
        }
    }

    fun analyzeStaticImageForBarcodes(inputImage: InputImage, imageUri: android.net.Uri?) {
        _uiState.value = BarcodeScanUiState.Loading // Indicate loading for image analysis
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
                val scanner = BarcodeScanning.getClient(options) // Use a new scanner for static images
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            onBarcodeDetected(barcodes, imageUri)
                        } else {
                            // No new barcodes found in the selected image, but analysis was successful
                            _uiState.value = BarcodeScanUiState.Error("No barcodes found in the selected image.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("BarcodeScannerVM", "Static image barcode scanning failed: ${e.message}", e)
                        _uiState.value = BarcodeScanUiState.Error("Error analyzing image: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("BarcodeScannerVM", "Unexpected error during static image analysis: ${e.message}", e)
                _uiState.value = BarcodeScanUiState.Error("Unexpected error during image analysis: ${e.message}")
            }
        }
    }

    fun onFlipCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            Log.d("BarcodeScreen", "Switching to front camera")
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            Log.d("BarcodeScreen", "Switching to back camera")
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // resetBottomSheetExpansion is removed. UI will manage its own sheet expansion.

    // Reset the UI state to Idle, typically after an error has been displayed
    fun resetUiState() {
        _uiState.value = BarcodeScanUiState.Idle
    }
}