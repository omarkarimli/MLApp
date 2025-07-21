package com.omarkarimli.mlapp.ui.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel // CHANGE: Extend ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class to hold an ImageLabel and its associated image URI (if from a picked image)
data class ImageLabelResult(
    val label: ImageLabel,
    val imageUri: Uri? = null // Nullable for live scans
)

// ViewModel for Image Labeling Screen
// CHANGE: Extend ViewModel, no constructor context
class ImageLabelingViewModel : ViewModel() {

    private val _hasCameraPermission = MutableStateFlow(false) // Initialized to false
    val hasCameraPermission = _hasCameraPermission.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false) // Initialized to false
    val hasStoragePermission = _hasStoragePermission.asStateFlow()

    private val _imageLabelResults = MutableStateFlow<List<ImageLabelResult>>(emptyList())
    val imageLabelResults = _imageLabelResults.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector = _cameraSelector.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun onCameraPermissionResult(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
        if (!isGranted) {
            viewModelScope.launch {
                _toastMessage.emit("Camera permission is required for live labeling.")
            }
        }
    }

    fun onStoragePermissionResult(isGranted: Boolean) {
        _hasStoragePermission.value = isGranted
        if (!isGranted) {
            viewModelScope.launch {
                _toastMessage.emit("Storage permission is required to pick photos.")
            }
        }
    }

    // CHANGE: Accepts Context parameter, mirrors BarcodeScanningViewModel's initializePermissions
    fun initializePermissions(context: Context) {
        _hasCameraPermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        _hasStoragePermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // CHANGE: Accepts Context parameter
    fun requestPermissions(context: Context, cameraPermissionLauncher: ActivityResultLauncher<String>, storagePermissionLauncher: ActivityResultLauncher<String>) {
        if (!hasCameraPermission.value) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasStoragePermission.value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // CHANGE: Accepts Context parameter
    fun analyzeImageFromUri(context: Context, uri: Uri?) {
        uri?.let {
            viewModelScope.launch {
                val bitmap: Bitmap? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    }
                } catch (e: Exception) {
                    Log.e("ImageLabelerViewModel", "Error decoding image: ${e.message}", e)
                    _toastMessage.emit("Failed to decode image from gallery.")
                    null
                }

                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

                    labeler.process(image)
                        .addOnSuccessListener { labels ->
                            val newLabelResults = labels.map { ImageLabelResult(it, uri) }
                            if (newLabelResults.isNotEmpty()) {
                                _imageLabelResults.update { currentList ->
                                    // Remove existing picked image results for this URI to avoid duplicates when re-picking same image
                                    val filteredList = currentList.filter { it.imageUri != uri }.toMutableList()
                                    // Add new labels, ensuring no exact duplicates from this specific URI
                                    newLabelResults.forEach { newLabel ->
                                        if (filteredList.none { existingLabel -> existingLabel.label.text == newLabel.label.text && existingLabel.imageUri == newLabel.imageUri }) {
                                            filteredList.add(newLabel)
                                        }
                                    }
                                    filteredList.toList()
                                }
                            } else {
                                viewModelScope.launch {
                                    _toastMessage.emit("No labels found in the selected image.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ImageLabelerViewModel", "Image labeling failed: ${e.message}", e)
                            viewModelScope.launch {
                                _toastMessage.emit("Error analyzing image: ${e.message}")
                            }
                        }
                }
            }
        }
    }

    fun onLiveLabelsDetected(labels: List<ImageLabel>) {
        _imageLabelResults.update { currentList ->
            val currentLiveLabels = currentList.filter { it.imageUri == null }.toSet()
            val newLiveLabels = labels.map { ImageLabelResult(it, null) }.toSet()

            if (newLiveLabels != currentLiveLabels) {
                val filteredList = currentList.filter { it.imageUri != null }.toMutableList()
                filteredList.addAll(newLiveLabels)
                filteredList.toList()
            } else {
                currentList // No change, return current list to avoid unnecessary recomposition
            }
        }
    }

    fun onFlipCameraClicked() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            Log.e("ImageLabelingViewModel", "Switching to front camera")
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            Log.e("ImageLabelingViewModel", "Switching to back camera")
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            _toastMessage.emit("Save functionality not implemented yet.")
        }
    }

    // REMOVED: No custom ViewModel.Factory as per BarcodeScanningViewModel
}