package com.omarkarimli.mlapp.ui.presentation.barcodescanning

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.barcode.common.Barcode
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.components.DetectedActionImage
import com.omarkarimli.mlapp.ui.presentation.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanningScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: BarcodeScanningViewModel = viewModel()

    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val barcodeResults by viewModel.barcodeResults.collectAsState()
    val cameraSelector by viewModel.cameraSelector.collectAsState()
    val shouldExpandBottomSheet by viewModel.shouldExpandBottomSheet.collectAsState()

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(SheetValue.PartiallyExpanded, skipHiddenState = true)
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required for live scanning.", Toast.LENGTH_SHORT).show()
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setStoragePermission(isGranted)
        if (!isGranted) {
            Toast.makeText(context, "Storage permission is required to pick photos.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializePermissions(context)
        if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            else
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.analyzeImageForBarcodes(context, it)
        }
    }

    LaunchedEffect(shouldExpandBottomSheet) {
        if (shouldExpandBottomSheet) {
            sheetScaffoldState.bottomSheetState.expand()
            viewModel.resetBottomSheetExpansion()
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
                        onClick = { /* Save - Business logic for saving would go in ViewModel */ },
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
        BottomSheetScaffold(
            modifier = Modifier.padding(paddingValues),
            scaffoldState = sheetScaffoldState,
            sheetPeekHeight = Dimens.BottomSheetPeekHeight,
            sheetContainerColor = MaterialTheme.colorScheme.background,
            sheetShape = RoundedCornerShape(topStart = Dimens.CornerRadiusLarge, topEnd = Dimens.CornerRadiusLarge),
            sheetDragHandle = { BottomSheetDefaults.DragHandle() },
            sheetContent = {
                BottomSheetContent(barcodeResults, context) {
                    viewModel.onFlipCamera()
                }
            },
            content = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasCameraPermission) {
                        CameraPreview(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Dimens.PaddingMedium).background(Color.Black, RoundedCornerShape(Dimens.CornerRadiusMedium)),
                            cameraSelector = cameraSelector,
                            onBarcodeDetected = { barcodes -> viewModel.onBarcodeDetected(barcodes) }
                        )
                    } else {
                        CameraPermissionPlaceholder(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(Dimens.PaddingMedium)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun CameraPreview(modifier: Modifier = Modifier, onBarcodeDetected: (List<Barcode>) -> Unit, cameraSelector: CameraSelector) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } },
        update = { previewView ->
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor,
                    BarcodeAnalyzer { barcodes -> onBarcodeDetected(barcodes) })
            }
            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("BarcodeScanner", "Use case binding failed", exc)
                Toast.makeText(context, "Error switching camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(barcodeResults: List<ScannedBarcode>, context: Context, onFlipCamera: () -> Unit) {
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
                    BarcodeResultCard(scannedBarcode, context)
                    if (index < barcodeResults.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BarcodeResultCard(scannedBarcode: ScannedBarcode, context: Context) {
    val barcode = scannedBarcode.barcode
    val imageUri = scannedBarcode.imageUri
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Format: ${getBarcodeFormatName(barcode.format)}", style = MaterialTheme.typography.bodyLarge)
            Text("Value: ${barcode.rawValue ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            Text("Type: ${getBarcodeValueTypeName(barcode.valueType)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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