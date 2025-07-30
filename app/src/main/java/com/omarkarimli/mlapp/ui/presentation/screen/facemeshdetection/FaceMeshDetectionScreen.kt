package com.omarkarimli.mlapp.ui.presentation.screen.facemeshdetection

import android.Manifest
import android.content.Intent
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.widget.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.utils.toResultCards
import com.omarkarimli.mlapp.ui.presentation.common.widget.BottomSheetContent
import com.omarkarimli.mlapp.ui.presentation.common.widget.CameraPreview
import com.omarkarimli.mlapp.ui.presentation.common.widget.ToggleButton
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import com.omarkarimli.mlapp.utils.showToast

@Preview(showBackground = true)
@Composable
fun FaceMeshDetectionScreenPreview() {
    MLAppTheme {
        FaceMeshDetectionScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceMeshDetectionScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: FaceMeshDetectionViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val cameraSelector by viewModel.cameraSelector.collectAsState()

    val faceMeshResults by viewModel.faceMeshResults.collectAsState()

    val imageSize by viewModel.imageSize.collectAsState()
    // State for the GraphicOverlay
    val graphicOverlay = remember { GraphicOverlayFaceMesh(context) }


    // Observe camera active state
    val isCameraActive by viewModel.isCameraActive.collectAsState()

    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(SheetValue.PartiallyExpanded, skipHiddenState = true)
    )

    LaunchedEffect(faceMeshResults, imageSize, cameraSelector) {
        graphicOverlay.clear()
        graphicOverlay.setImageSourceInfo(imageSize.width, imageSize.height)
        faceMeshResults.forEach { scannedFaceMesh ->
            // Pass true for front camera, false for back camera
            if (scannedFaceMesh.imageUri == null) {
                graphicOverlay.add(FaceMeshGraphic(graphicOverlay, scannedFaceMesh.faceMesh, cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA))
            }
        }
        graphicOverlay.postInvalidate() // Request redraw for the overlay
    }

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
                        viewModel.analyzeStaticImageForObjects(inputImage, imageUri)
                    }
                } catch (e: Exception) {
                    Log.e("FaceMeshDetectionScreen", "Error decoding image from gallery: ${e.message}", e)
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
            is UiState.Error -> {
                val errorMessage = (uiState as UiState.Error).message
                context.showToast(errorMessage)
                Log.e("FaceMeshDetectionScreen", "Error: $errorMessage")

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
                    Text(Screen.FaceMeshDetection.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        modifier = Modifier
                            .width(Dimens.IconSizeExtraLarge)
                            .height(Dimens.IconSizeLarge),
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
                    BottomSheetContent(
                        faceMeshResults.toResultCards(),
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
                                        viewModel.analyzeLiveFaceMesh(
                                            imageProxy
                                        )
                                    },
                                    graphicOverlay = {
                                        AndroidView(
                                            factory = { graphicOverlay },
                                            modifier = Modifier.fillMaxSize()
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