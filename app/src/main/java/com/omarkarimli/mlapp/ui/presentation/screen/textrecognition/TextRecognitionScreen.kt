package com.omarkarimli.mlapp.ui.presentation.screen.textrecognition // Changed package

import android.Manifest
import android.content.Intent
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import com.omarkarimli.mlapp.ui.presentation.common.widget.BottomSheetContent
import com.omarkarimli.mlapp.ui.presentation.common.widget.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.presentation.common.widget.CameraPreview
import com.omarkarimli.mlapp.ui.presentation.common.widget.ToggleButton
import com.omarkarimli.mlapp.utils.Dimens
import com.omarkarimli.mlapp.utils.showToast
import com.omarkarimli.mlapp.utils.toResultCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextRecognitionScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Inject TextRecognitionViewModel
    val viewModel: TextRecognitionViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsState()
    // Observe recognized text result
    val textResults by viewModel.textResults.collectAsState()

    // Observe camera active state
    val isCameraActive by viewModel.isCameraActive.collectAsState()
    val cameraSelector by viewModel.cameraSelector.collectAsState()

    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(SheetValue.PartiallyExpanded, skipHiddenState = true)
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.permissionRepository.notifyPermissionChanged(Manifest.permission.CAMERA)
        if (!isGranted) {
            context.showToast("Camera permission is required for live scanning.")
        }
        viewModel.resetUiState()
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            // --- IMPORTANT: Persist URI permission here ---
            // Flags for read and write access, depending on what you need.
            // For displaying, Intent.FLAG_GRANT_READ_URI_PERMISSION is sufficient.
            val takeFlag: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION // or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

            try {
                // Request persistable permission
                context.contentResolver.takePersistableUriPermission(imageUri, takeFlag)
                Log.d("BarcodeScreen", "Persisted URI permission for: $imageUri")
            } catch (e: SecurityException) {
                Log.e("BarcodeScreen", "Failed to persist URI permission: ${e.message}", e)
                context.showToast("Failed to get persistent access to the image.")
                return@let // Exit if permission cannot be persisted
            }
            // --- End of IMPORTANT section ---

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
                    } else {
                        @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                    }
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    withContext(Dispatchers.Main) {
                        // Call analyzeStaticImageForText
                        viewModel.analyzeStaticImageForText(inputImage, imageUri)
                    }
                } catch (e: Exception) {
                    Log.e("TextRecognitionScreen", "Error decoding image from gallery: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        context.showToast("Failed to load image: ${e.message}")
                    }
                }
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.permissionRepository.notifyPermissionChanged(viewModel.permissionRepository.getStoragePermission())
        if (isGranted) {
            // Permission granted, now launch the image picker
            coroutineScope.launch { sheetScaffoldState.bottomSheetState.partialExpand() }
            // Ensure this is called only if granted
            pickImageLauncher.launch("image/*")
        } else {
            context.showToast("Storage permission is required to pick photos.")
        }
        viewModel.resetUiState()
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            UiState.Loading -> { /* Handle loading if needed */ }
            is UiState.Success -> { /* Handle success if needed */ }
            is UiState.Error -> {
                val errorMessage = (uiState as UiState.Error).message
                context.showToast(errorMessage)
                Log.e("TextRecognitionScreen", "Error: $errorMessage") // Changed Log tag

                viewModel.resetUiState()
            }
            is UiState.PermissionAction -> {
                val permissionToRequest = (uiState as UiState.PermissionAction).permission
                when (permissionToRequest) {
                    Manifest.permission.CAMERA -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    // This is where storage permission will be handled
                    viewModel.permissionRepository.getStoragePermission() -> storagePermissionLauncher.launch(
                        viewModel.permissionRepository.getStoragePermission()
                    )
                }

                viewModel.resetUiState()
            }
            UiState.Idle -> {
                // Hide any loading indicators
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Changed title to TextRecognition
                    Text(Screen.TextRecognition.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ToggleButton(
                        isToggled = isCameraActive,
                        onToggle = { viewModel.toggleCameraActive() }
                    )
                    Spacer(Modifier.size(Dimens.SpacerSmall))
                    FilledIconButton(
                        onClick = {
                            if (isCameraActive) viewModel.toggleCameraActive()

                            if (hasStoragePermission) {
                                coroutineScope.launch { sheetScaffoldState.bottomSheetState.partialExpand() }
                                pickImageLauncher.launch("image/*")
                            } else {
                                // Request permission directly from the UI here
                                storagePermissionLauncher.launch(viewModel.permissionRepository.getStoragePermission())
                            }
                        },
                        modifier = Modifier.size(Dimens.IconSizeLarge),
                        shape = IconButtonDefaults.filledShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Photo, modifier = Modifier.size(Dimens.IconSizeSmall), contentDescription = "Pick Photo")
                    }
                    Spacer(Modifier.size(Dimens.SpacerSmall))
                    FilledTonalIconButton(
                        onClick = {
                            // Call the new save function in the ViewModel
                            viewModel.saveCurrentResults()
                            context.showToast("Results saved!") // Provide feedback
                        },
                        modifier = Modifier.width(Dimens.IconSizeExtraLarge).height(Dimens.IconSizeLarge),
                        shape = IconButtonDefaults.filledShape
                    ) {
                        Icon(Icons.Rounded.Done, modifier = Modifier.size(Dimens.IconSizeSmall), contentDescription = "Save")
                    }
                    Spacer(Modifier.size(Dimens.SpacerSmall))
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            BottomSheetScaffold(
                modifier = Modifier.padding(paddingValues),
                scaffoldState = sheetScaffoldState,
                sheetPeekHeight = Dimens.BottomSheetPeekHeight,
                sheetContainerColor = MaterialTheme.colorScheme.background,
                sheetShape = RoundedCornerShape(topStart = Dimens.CornerRadiusLarge, topEnd = Dimens.CornerRadiusLarge),
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                sheetContent = {
                    // Pass the recognized text converted to ResultCard list
                    BottomSheetContent(
                        textResults.toResultCards(),
                        onFlipCamera = { viewModel.onFlipCamera() },
                        isCameraActive = isCameraActive
                    )
                },
                content = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasCameraPermission) {
                            if (isCameraActive) {
                                CameraPreview(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(
                                            Color.Black,
                                            RoundedCornerShape(Dimens.CornerRadiusMedium)
                                        ),
                                    cameraSelector = cameraSelector,
                                    analyzeLive = { imageProxy ->
                                        // Call analyzeLiveText
                                        viewModel.analyzeLiveText(
                                            imageProxy
                                        )
                                    }
                                )
                            } else {
                                Text("Camera has been paused", modifier = Modifier.padding(Dimens.PaddingMedium))
                            }
                        } else {
                            CameraPermissionPlaceholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(Dimens.PaddingMedium),
                                onPermissionRequested = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}
