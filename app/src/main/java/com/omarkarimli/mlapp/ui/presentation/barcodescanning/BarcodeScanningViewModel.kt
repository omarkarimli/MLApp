package com.omarkarimli.mlapp.ui.presentation.barcodescanning

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannedBarcode(val barcode: Barcode, val imageUri: Uri? = null)

class BarcodeScanningViewModel : ViewModel() {

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _barcodeResults = MutableStateFlow<MutableList<ScannedBarcode>>(mutableListOf())
    val barcodeResults: StateFlow<List<ScannedBarcode>> = _barcodeResults.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    // Flag to indicate if the bottom sheet should expand (e.g., after new barcodes are added)
    private val _shouldExpandBottomSheet = MutableStateFlow(false)
    val shouldExpandBottomSheet: StateFlow<Boolean> = _shouldExpandBottomSheet.asStateFlow()

    // Barcode scanner instance for live camera analysis
    private val barcodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
    private val liveBarcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

    fun setCameraPermission(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
    }

    fun setStoragePermission(isGranted: Boolean) {
        _hasStoragePermission.value = isGranted
    }

    fun initializePermissions(context: Context) {
        _hasCameraPermission.value = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        _hasStoragePermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun onBarcodeDetected(barcodes: List<Barcode>) {
        val currentResults = _barcodeResults.value
        val newBarcodes = barcodes.filter { newBarcode ->
            currentResults.none { it.barcode.rawValue == newBarcode.rawValue }
        }.map { ScannedBarcode(it, null) }

        if (newBarcodes.isNotEmpty()) {
            _barcodeResults.update { (it + newBarcodes).toMutableList() }
            _shouldExpandBottomSheet.value = true
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun analyzeLiveBarcode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            liveBarcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) onBarcodeDetected(barcodes)
                }
                .addOnFailureListener { e -> Log.e("BarcodeScannerVM", "Live barcode scanning failed: ${e.message}", e) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    fun analyzeImageForBarcodes(context: Context, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
                    else
                        @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                } catch (e: Exception) {
                    Log.e("BarcodeScannerVM", "Error decoding image: ${e.message}", e)
                    null
                }

                bitmap?.let {
                    val image = InputImage.fromBitmap(it, 0)
                    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
                    val scanner = BarcodeScanning.getClient(options) // Use a new scanner for static images
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val scannedBarcodes = barcodes.map { currentBarcode ->
                                ScannedBarcode(
                                    currentBarcode,
                                    imageUri
                                )
                            }
                            val currentResults = _barcodeResults.value
                            val newBarcodes = scannedBarcodes.filter { newBarcode ->
                                currentResults.none { prevBarcode -> prevBarcode.barcode.rawValue == newBarcode.barcode.rawValue }
                            }
                            if (newBarcodes.isNotEmpty()) {
                                _barcodeResults.update { prevBarcode -> (prevBarcode + newBarcodes).toMutableList() }
                                _shouldExpandBottomSheet.value = true
                            }
                            if (barcodes.isEmpty()) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    Toast.makeText(context, "No barcodes found in the selected image.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("BarcodeScannerVM", "Image barcode scanning failed: ${e.message}", e)
                            viewModelScope.launch(Dispatchers.Main) {
                                Toast.makeText(context, "Error analyzing image: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } ?: withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to decode image from gallery.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("BarcodeScannerVM", "Unexpected error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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

    // Reset the shouldExpandBottomSheet flag after it's been consumed by the UI
    fun resetBottomSheetExpansion() {
        _shouldExpandBottomSheet.value = false
    }
}