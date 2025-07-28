package com.omarkarimli.mlapp.ui.presentation.ui.objectdetection

import android.Manifest
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.ui.common.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.objects.DetectedObject
import com.omarkarimli.mlapp.domain.models.toResultCards
import com.omarkarimli.mlapp.ui.presentation.ui.common.BottomSheetContent
import com.omarkarimli.mlapp.ui.presentation.ui.common.CameraPreview
import com.omarkarimli.mlapp.ui.presentation.ui.common.UiState
import com.omarkarimli.mlapp.utils.showToast

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ObjectDetectionScreenPreview() {
    MLAppTheme {
        ObjectDetectionScreen(navController = NavHostController(LocalContext.current))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetectionScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: ObjectDetectionViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val cameraSelector by viewModel.cameraSelector.collectAsState()

    var imageSize by remember { mutableStateOf(Size(1,1)) }
    val objectResults by viewModel.objectResults.collectAsState()

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
                    Log.e("ObjectDetectionScreen", "Error decoding image from gallery: ${e.message}", e)
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
                Log.e("ObjectDetectionScreen", "Error: $errorMessage")

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
                    Text(Screen.BarcodeScanning.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = {
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
                            context.showToast("Save functionality to be implemented")
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
                    BottomSheetContent(objectResults.toResultCards(), onFlipCamera = {
                        viewModel.onFlipCamera()
                    })
                },
                content = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasCameraPermission) {
                            // map objectResults to detectedObjects
                            val detectedObjects: List<DetectedObject> = objectResults.map { scannedObject ->
                                scannedObject.detectedObject
                            }

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
                                    imageSize = Size(imageProxy.width, imageProxy.height)
                                    viewModel.analyzeLiveObject(
                                        imageProxy
                                    )
                                },
                                graphicOverlay = {
                                    GraphicOverlayObject(
                                        detectedObjects = detectedObjects,
                                        imageSize = imageSize,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            )
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