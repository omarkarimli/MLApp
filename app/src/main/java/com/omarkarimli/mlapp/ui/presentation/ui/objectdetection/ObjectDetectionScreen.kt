package com.omarkarimli.mlapp.ui.presentation.ui.objectdetection

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.FlipCameraIos
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.ui.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.presentation.ui.components.DetectedActionImage
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Constants
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.min

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

    val viewModel: ObjectDetectionViewModel = viewModel()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val objectResults = viewModel.objectResults // This is already a mutableStateListOf, no need for by
    val cameraSelector by viewModel.cameraSelector.collectAsState()
    val detectedObjectsForOverlay by viewModel.detectedObjectsForOverlay.collectAsState()
    val currentImageSize by viewModel.currentImageSize.collectAsState()

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // Collect Toast messages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.updateCameraPermission(isGranted)
        }
    )

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.updateStoragePermission(isGranted)
        }
    )

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.onImagePicked(context, uri)
            // Expand sheet if results are available from picked image
            if (objectResults.any { it.imageUri != null }) { // Check if there are results specifically from picked images
                coroutineScope.launch {
                    if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                        sheetScaffoldState.bottomSheetState.expand()
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        // Initial permission requests
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Screen.ObjectDetection.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                                coroutineScope.launch {
                                    // Ensure sheet is at least partially visible before launching picker
                                    if (sheetScaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                                        sheetScaffoldState.bottomSheetState.partialExpand()
                                    }
                                }
                                pickImageLauncher.launch("image/*")
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        },
                        modifier = Modifier.size(Dimens.IconSizeLarge),
                        shape = IconButtonDefaults.filledShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Photo,
                            modifier = Modifier.size(Dimens.IconSizeSmall),
                            contentDescription = "Pick Photo"
                        )
                    }
                    Spacer(Modifier.size(Dimens.SpacerSmall))
                    FilledTonalIconButton(
                        onClick = { viewModel.onSaveClicked() },
                        modifier = Modifier
                            .width(Dimens.IconSizeExtraLarge)
                            .height(Dimens.IconSizeLarge),
                        shape = IconButtonDefaults.filledShape
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            modifier = Modifier.size(Dimens.IconSizeSmall),
                            contentDescription = "Save",
                        )
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
            sheetDragHandle = {
                BottomSheetDefaults.DragHandle()
            },
            sheetContent = {
                BottomSheetContentObjects(
                    objectResults = objectResults,
                    context = context,
                    onFlipCamera = {
                        viewModel.onFlipCamera()
                    }
                )
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasCameraPermission) {
                        CameraPreview(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black, RoundedCornerShape(Dimens.CornerRadiusMedium)),
                            cameraSelector = cameraSelector,
                            detectedObjects = detectedObjectsForOverlay,
                            imageSize = currentImageSize,
                            onObjectsDetected = { objects, imageWidth, imageHeight ->
                                viewModel.onObjectsDetected(objects, imageWidth, imageHeight)
                                if (objects.isNotEmpty()) {
                                    coroutineScope.launch {
                                        // Only partially expand if the sheet is currently hidden or collapsed
                                        if (sheetScaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                                            sheetScaffoldState.bottomSheetState.partialExpand()
                                        }
                                    }
                                }
                            }
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
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector,
    detectedObjects: List<DetectedObject>, // Passed from ViewModel
    imageSize: Size, // Passed from ViewModel
    onObjectsDetected: (List<DetectedObject>, Int, Int) -> Unit // Callback to ViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Initialize the object detector outside of the factory/update blocks
    // so it's only created once per Composable lifecycle
    val objectDetector = remember {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FIT_START
                }
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                                    objectDetector.process(image)
                                        .addOnSuccessListener { detectedObjects ->
                                            onObjectsDetected(detectedObjects, imageProxy.width, imageProxy.height)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("ObjectDetector", "Object detection failed: ${e.message}", e)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        Log.e("ObjectDetector", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                val cameraProvider = cameraProviderFuture.get()

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                                objectDetector.process(image)
                                    .addOnSuccessListener { detectedObjectsResult ->
                                        onObjectsDetected(detectedObjectsResult, imageProxy.width, imageProxy.height)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ObjectDetector", "Object detection failed: ${e.message}", e)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("BarcodeScanner", "Use case binding failed", exc)
                    Toast.makeText(context, "Error switching camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        GraphicOverlay(
            detectedObjects = detectedObjects,
            imageSize = imageSize,
            modifier = Modifier.fillMaxSize()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            // Important: Close the ObjectDetector to release resources
            objectDetector.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContentObjects(objectResults: List<ScannedObject>, context: Context, onFlipCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingMedium)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Detected Objects",
                modifier = Modifier.padding(bottom = Dimens.PaddingSmall),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier.size(Dimens.IconSizeLarge)
            ) {
                Icon(Icons.Rounded.FlipCameraIos, contentDescription = "Flip Camera")
            }
        }

        if (objectResults.isEmpty()) {
            Text(
                "No objects detected yet. Scan live or pick an image.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingSmall),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.BarcodeListMaxHeight),
                contentPadding = PaddingValues(vertical = Dimens.PaddingExtraSmall)
            ) {
                itemsIndexed(objectResults) { index, scannedObject ->
                    ObjectResultCard(scannedObject = scannedObject, context = context)
                    if (index < objectResults.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ObjectResultCard(scannedObject: ScannedObject, context: Context) {
    val detectedObject = scannedObject.detectedObject
    val imageUri = scannedObject.imageUri

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Bounding Box: ${detectedObject.boundingBox}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            detectedObject.labels.forEach { label ->
                Text(text = "Label: ${label.text} (${"%.2f".format(label.confidence)})", style = MaterialTheme.typography.bodyLarge)
            }
            if (detectedObject.labels.isEmpty()) {
                Text(text = "Label: N/A", style = MaterialTheme.typography.bodyLarge)
            }
        }

        DetectedActionImage(context, imageUri)
    }
}

@Composable
private fun GraphicOverlay(detectedObjects: List<DetectedObject>, imageSize: Size, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageSize.width <= 0 || imageSize.height <= 0) return@Canvas

        val scaleFactor = min(
            size.width / imageSize.width.toFloat(),
            size.height / imageSize.height.toFloat()
        )

        val offsetX = (size.width - imageSize.width * scaleFactor) / 2
        val offsetY = (size.height - imageSize.height * scaleFactor) / 2

        detectedObjects.forEach { detectedObject ->
            val boundingBox = detectedObject.boundingBox

            val left = boundingBox.left * scaleFactor + offsetX
            val top = boundingBox.top * scaleFactor + offsetY
            val right = boundingBox.right * scaleFactor + offsetX
            val bottom = boundingBox.bottom * scaleFactor + offsetY

            drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = Dimens.DrawLineStrokeWidth)
            )

            val labelText = detectedObject.labels.firstOrNull()?.text ?: Constants.NOT_APPLICABLE
            val confidence = detectedObject.labels.firstOrNull()?.confidence?.let {
                "%.1f%%".format(it * 100)
            } ?: Constants.NOT_APPLICABLE
            val fullText = "$labelText ($confidence)"

            val textPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = Dimens.DrawLabelTextSize
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }

            val bgPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val textX = left
            val textY = top - 10f

            val textWidth = textPaint.measureText(fullText)

            drawContext.canvas.nativeCanvas.drawRect(
                textX - 5f,
                textY - 50f,
                textX + textWidth + 5f,
                textY + 5f,
                bgPaint
            )

            drawContext.canvas.nativeCanvas.drawText(
                fullText,
                textX,
                textY,
                textPaint
            )
        }
    }
}