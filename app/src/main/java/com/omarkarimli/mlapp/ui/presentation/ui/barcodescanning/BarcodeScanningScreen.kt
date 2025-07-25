package com.omarkarimli.mlapp.ui.presentation.ui.barcodescanning

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.FlipCameraIos
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.omarkarimli.mlapp.domain.models.ScannedBarcode
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.ui.components.DetectedActionImage
import com.omarkarimli.mlapp.ui.presentation.ui.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.presentation.ui.components.CustomToast
import com.omarkarimli.mlapp.ui.presentation.ui.components.CustomToastState
import com.omarkarimli.mlapp.ui.presentation.ui.components.CustomToastType
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import com.omarkarimli.mlapp.utils.showCustomToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanningScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Needs to be remembered for the extension function

    val viewModel: BarcodeScanningViewModel = viewModel()

    val uiState by viewModel.uiState.collectAsState()
    val barcodeResults by viewModel.barcodeResults.collectAsState()
    val cameraSelector by viewModel.cameraSelector.collectAsState()

    // UI-managed permission states
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    // Custom toast state
    val customToastState = remember { mutableStateOf(CustomToastState("", CustomToastType.INFO)) } // Use mutableStateOf

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(SheetValue.PartiallyExpanded, skipHiddenState = true)
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted // Update UI state
        if (!isGranted) {
            coroutineScope.showCustomToast(customToastState, "Camera permission is required for live scanning.", CustomToastType.ERROR)
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted // Update UI state
        if (!isGranted) {
            coroutineScope.showCustomToast(customToastState, "Storage permission is required to pick photos.", CustomToastType.ERROR)
        }
    }

    // Initial permission checks and requests
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            else
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Observe UI state for feedback (loading, errors)
    LaunchedEffect(uiState) {
        when (uiState) {
            BarcodeScanningViewModel.BarcodeScanUiState.Loading -> {
                // coroutineScope.showCustomToast(toastState = customToastState, type = CustomToastType.INFO)
            }
            is BarcodeScanningViewModel.BarcodeScanUiState.Error -> {
                val errorMessage = (uiState as BarcodeScanningViewModel.BarcodeScanUiState.Error).message
                coroutineScope.showCustomToast(customToastState, errorMessage, CustomToastType.ERROR)
                viewModel.resetUiState() // Reset the error state after showing the Toast
            }
            BarcodeScanningViewModel.BarcodeScanUiState.Idle -> {
                // Hide any loading indicators if they were shown
            }
        }
    }

    // Image picker logic: converts Uri to InputImage before passing to ViewModel
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            coroutineScope.launch(Dispatchers.IO) { // Use IO dispatcher for image decoding
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
                    } else {
                        @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                    }
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    withContext(Dispatchers.Main) {
                        viewModel.analyzeStaticImageForBarcodes(inputImage, imageUri)
                    }
                } catch (e: Exception) {
                    Log.e("BarcodeScreen", "Error decoding image from gallery: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        coroutineScope.showCustomToast(customToastState, "Failed to load image: ${e.message}", CustomToastType.ERROR)
                    }
                }
            }
        }
    }

    // React to changes in barcodeResults size to expand the bottom sheet
    val barcodeResultCount by remember(barcodeResults) {
        derivedStateOf { barcodeResults.size }
    }
    LaunchedEffect(barcodeResultCount) {
        // Only expand if new barcodes were added and the sheet isn't already expanded/fully hidden
        if (barcodeResultCount > 0 && sheetScaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
            coroutineScope.launch {
                sheetScaffoldState.bottomSheetState.expand()
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
                                // Request permission if not granted
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                else
                                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
                            coroutineScope.showCustomToast(customToastState, "Save functionality to be implemented", CustomToastType.INFO)
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
        Box(modifier = Modifier.fillMaxSize()) { // Use Box for layering
            BottomSheetScaffold(
                modifier = Modifier.padding(paddingValues),
                scaffoldState = sheetScaffoldState,
                sheetPeekHeight = Dimens.BottomSheetPeekHeight,
                sheetContainerColor = MaterialTheme.colorScheme.background,
                sheetShape = RoundedCornerShape(topStart = Dimens.CornerRadiusLarge, topEnd = Dimens.CornerRadiusLarge),
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                sheetContent = {
                    BottomSheetContent(barcodeResults, onFlipCamera = {
                        viewModel.onFlipCamera()
                    })
                },
                content = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasCameraPermission) {
                            CameraPreview(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color.Black, RoundedCornerShape(Dimens.CornerRadiusMedium)),
                                cameraSelector = cameraSelector,
                                analyzeLiveBarcode = { imageProxy -> viewModel.analyzeLiveBarcode(imageProxy) }
                            )
                        } else {
                            CameraPermissionPlaceholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(Dimens.PaddingMedium)
                            )
                        }
                    }
                }
            )

            CustomToast(
                toastState = customToastState,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                        top = paddingValues.calculateTopPadding() + Dimens.PaddingMedium,
                        start = Dimens.PaddingMedium,
                        end = Dimens.PaddingMedium
                    )
                    .align(Alignment.TopCenter) // Center the toast within this smaller container
            )
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier = Modifier, cameraSelector: CameraSelector, analyzeLiveBarcode: (ImageProxy) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FIT_START } },
        update = { previewView ->
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeLiveBarcode(imageProxy)
                    }
                }
            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("BarcodeScanner", "Use case binding failed", exc)
                // ViewModel will handle errors via uiState
            }
        }
    )

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(barcodeResults: List<ScannedBarcode>, onFlipCamera: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PaddingMedium), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Detected Barcodes", modifier = Modifier.padding(bottom = Dimens.PaddingSmall), textAlign = TextAlign.Start, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onFlipCamera, modifier = Modifier.size(Dimens.IconSizeLarge)) {
                Icon(Icons.Rounded.FlipCameraIos, contentDescription = "Flip Camera")
            }
        }
        if (barcodeResults.isEmpty()) {
            Text("No barcodes detected yet. Scan live or pick an image.", modifier = Modifier.fillMaxWidth().padding(Dimens.PaddingSmall), textAlign = TextAlign.Start, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.BarcodeListMaxHeight), contentPadding = PaddingValues(vertical = Dimens.PaddingExtraSmall)) {
                itemsIndexed(barcodeResults) { index, scannedBarcode ->
                    BarcodeResultCard(scannedBarcode)
                    if (index < barcodeResults.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BarcodeResultCard(scannedBarcode: ScannedBarcode) {
    val barcode = scannedBarcode.barcode
    val imageUri = scannedBarcode.imageUri
    val context = LocalContext.current // Get context locally for DetectedActionImage

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Format: ${getBarcodeFormatName(barcode.format)} (${getBarcodeValueTypeName(barcode.valueType)})", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(barcode.rawValue ?: "N/A", style = MaterialTheme.typography.bodyLarge)
        }
        DetectedActionImage(context, imageUri)
    }
}

private fun getBarcodeFormatName(format: Int): String = when (format) {
    Barcode.FORMAT_AZTEC -> "AZTEC"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_CODE_39 -> "CODE_39"
    Barcode.FORMAT_CODE_93 -> "CODE_93"
    Barcode.FORMAT_CODE_128 -> "CODE_128"
    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
    Barcode.FORMAT_EAN_8 -> "EAN_8"
    Barcode.FORMAT_EAN_13 -> "EAN_13"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_QR_CODE -> "QR_CODE"
    Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_ALL_FORMATS -> "ALL_FORMATS"
    else -> "UNKNOWN"
}

private fun getBarcodeValueTypeName(type: Int): String = when (type) {
    Barcode.TYPE_CONTACT_INFO -> "CONTACT_INFO"
    Barcode.TYPE_EMAIL -> "EMAIL"
    Barcode.TYPE_ISBN -> "ISBN"
    Barcode.TYPE_PHONE -> "PHONE"
    Barcode.TYPE_PRODUCT -> "PRODUCT"
    Barcode.TYPE_SMS -> "SMS"
    Barcode.TYPE_TEXT -> "TEXT"
    Barcode.TYPE_URL -> "URL"
    Barcode.TYPE_WIFI -> "WIFI"
    Barcode.TYPE_GEO -> "GEO"
    Barcode.TYPE_CALENDAR_EVENT -> "CALENDAR_EVENT"
    Barcode.TYPE_DRIVER_LICENSE -> "DRIVER_LICENSE"
    else -> "UNKNOWN"
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun BarcodeScanningScreenPreview() {
    MLAppTheme {
        BarcodeScanningScreen(navController = NavHostController(LocalContext.current))
    }
}