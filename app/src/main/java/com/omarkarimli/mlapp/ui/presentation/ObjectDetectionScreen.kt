package com.omarkarimli.mlapp.ui.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.presentation.components.DetectedActionImage
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Constants
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.min

// Data class to hold a DetectedObject and its associated image URI (if from a picked image)
data class ScannedObject(
    val detectedObject: DetectedObject,
    val imageUri: Uri? = null // Nullable for live scans
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetectionScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Camera permission is required for live detection.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasStoragePermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Storage permission is required to pick photos.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
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

    val objectResults = remember { mutableStateListOf<ScannedObject>() }

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    analyzeImageForObjects(context, it) { results ->
                        // Clear existing live scan results when a new image is picked
                        objectResults.removeAll { scannedObject ->  scannedObject.imageUri == null }

                        results.forEach { newScannedObject ->
                            // Simple check to avoid duplicates for now, based on label if available
                            val newLabel = newScannedObject.detectedObject.labels.firstOrNull()?.text
                            // Improved duplicate check for picked images based on label and URI
                            if (objectResults.none { prevScannedObject ->
                                    prevScannedObject.detectedObject.labels.firstOrNull()?.text == newLabel && prevScannedObject.imageUri == newScannedObject.imageUri
                                }) {
                                objectResults.add(newScannedObject)
                            }
                        }
                        if (results.isNotEmpty()) {
                            coroutineScope.launch {
                                // Only expand if the sheet is not already expanded or hidden
                                if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                                    sheetScaffoldState.bottomSheetState.expand()
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // State to hold the current camera selector (front or back)
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

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
                        onClick = { /* TODO: Implement save functionality for detected objects */
                            Toast.makeText(context, "Save functionality not implemented yet.", Toast.LENGTH_SHORT).show()
                        },
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
                        // Toggle camera selector
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            Log.e("ObjectDetectionScreen", "Switching to front camera")
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            Log.e("ObjectDetectionScreen", "Switching to back camera")
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
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
                        CameraPreviewObjectDetection(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black, RoundedCornerShape(Dimens.CornerRadiusMedium)),
                            cameraSelector = cameraSelector,
                            onObjectsDetected = { objects, imageSize -> // Now receives imageSize
                                // Filter out live scan results, then add new ones
                                val currentLiveObjects = objectResults.filter { it.imageUri == null }.map { it.detectedObject.labels.firstOrNull()?.text to it.detectedObject.boundingBox }.toSet()
                                val newLiveObjects = objects.map { it.labels.firstOrNull()?.text to it.boundingBox }.toSet()

                                if (newLiveObjects != currentLiveObjects) {
                                    objectResults.removeAll { it.imageUri == null } // Remove old live scan results
                                    objects.forEach { newObject ->
                                        // Only add if it's genuinely new to the live scan list
                                        if (objectResults.none { existingObject ->
                                                existingObject.imageUri == null &&
                                                        existingObject.detectedObject.labels.firstOrNull()?.text == newObject.labels.firstOrNull()?.text &&
                                                        existingObject.detectedObject.boundingBox == newObject.boundingBox
                                            }) {
                                            objectResults.add(ScannedObject(newObject, null))
                                        }
                                    }

                                    coroutineScope.launch {
                                        if (objects.isNotEmpty()) {
                                            // Only partially expand if the sheet is currently hidden or collapsed
                                            if (sheetScaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                                                sheetScaffoldState.bottomSheetState.partialExpand()
                                            }
                                        }
                                    }
                                }
                                // Note: The imageSize is used internally by CameraPreviewObjectDetection
                                // for the GraphicOverlay, so it's not directly needed here for objectResults list.
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

@Composable
fun CameraPreviewObjectDetection(
    modifier: Modifier = Modifier,
    onObjectsDetected: (List<DetectedObject>, Size) -> Unit, // Modified: now also passes image size
    cameraSelector: CameraSelector
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // State to hold the latest detected objects and image size for the overlay
    var detectedObjectsForOverlay by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var currentImageSize by remember { mutableStateOf(Size(1, 1)) } // Default to a small size

    Box(modifier = modifier) { // Use Box to layer the preview and overlay
        AndroidView(
            modifier = Modifier.fillMaxSize(), // Fill the box
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FIT_CENTER
                }
                // Add listener to cameraProviderFuture to bind camera once available
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
                            it.setAnalyzer(cameraExecutor, ObjectDetectorAnalyzer { objects, imageWidth, imageHeight -> // Modified: get image dimensions
                                detectedObjectsForOverlay = objects
                                currentImageSize = Size(imageWidth, imageHeight)
                                onObjectsDetected(objects, Size(imageWidth, imageHeight)) // Pass to parent
                            })
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector, // Use the passed cameraSelector here for initial binding
                            preview,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        Log.e("ObjectDetector", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx)) // Use main executor for the listener

                previewView
            },
            update = { previewView -> // This block runs on recomposition when cameraSelector changes
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
                        it.setAnalyzer(cameraExecutor, ObjectDetectorAnalyzer { detectedObjects, imageWidth, imageHeight -> // Modified: get image dimensions
                            detectedObjectsForOverlay = detectedObjects
                            currentImageSize = Size(imageWidth, imageHeight)
                            onObjectsDetected(detectedObjects, Size(imageWidth, imageHeight)) // Pass to parent
                        })
                    }

                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector, // Use the updated cameraSelector
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("BarcodeScanner", "Use case binding failed", exc)
                    Toast.makeText(context, "Error switching camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Draw the graphic overlay on top of the preview
        GraphicOverlay(
            detectedObjects = detectedObjectsForOverlay,
            imageSize = currentImageSize,
            modifier = Modifier.fillMaxSize()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContentObjects(objectResults: List<ScannedObject>, context: Context, onFlipCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingMedium)
            .background(MaterialTheme.colorScheme.background), // Ensure background for drag handle visibility
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
                onClick = onFlipCamera, // Call the onFlipCamera lambda
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
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium)) // Add some space if empty
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.BarcodeListMaxHeight), // RE-ADDED this line
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

class ObjectDetectorAnalyzer(private val listener: (List<DetectedObject>, Int, Int) -> Unit) : ImageAnalysis.Analyzer { // Modified listener signature
    // Default object detector options
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val objectDetector = ObjectDetection.getClient(options)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    // Pass image width and height
                    listener(detectedObjects, imageProxy.width, imageProxy.height)
                }
                .addOnFailureListener { e ->
                    Log.e("ObjectDetector", "Object detection failed: ${e.message}", e)
                }
                .addOnCompleteListener {
                    imageProxy.close() // Always close the imageProxy
                }
        } else {
            imageProxy.close() // Always close the imageProxy even if mediaImage is null
        }
    }
}

suspend fun analyzeImageForObjects(
    context: Context,
    imageUri: Uri,
    onResult: (List<ScannedObject>) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val bitmap: Bitmap? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                }
            } catch (e: Exception) {
                Log.e("ObjectDetector", "Error decoding image: ${e.message}", e)
                null
            }

            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)

                // Default object detector options for image processing
                val options = ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .enableClassification()
                    .build()

                val objectDetector = ObjectDetection.getClient(options)

                objectDetector.process(image)
                    .addOnSuccessListener { detectedObjects ->
                        val scannedObjects = detectedObjects.map { ScannedObject(it, imageUri) }
                        onResult(scannedObjects)
                        if (detectedObjects.isEmpty()) {
                            Toast.makeText(context, "No objects found in the selected image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ObjectDetector", "Image object detection failed: ${e.message}", e)
                        Toast.makeText(context, "Error analyzing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to decode image from gallery.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Unexpected error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun ObjectResultCard(scannedObject: ScannedObject, context: Context) {
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
                Text(text = "Label: ${label.text}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Confidence: ${"%.2f".format(label.confidence)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (detectedObject.labels.isEmpty()) {
                Text(text = "Label: N/A", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Confidence: N/A", style = MaterialTheme.typography.bodyMedium)
            }
        }

        DetectedActionImage(context, imageUri)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ObjectDetectionScreenPreview() {
    MLAppTheme {
        ObjectDetectionScreen(navController = NavHostController(LocalContext.current))
    }
}

@Composable
private fun GraphicOverlay(
    detectedObjects: List<DetectedObject>,
    imageSize: Size,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageSize.width <= 0 || imageSize.height <= 0) return@Canvas

        // Calculate scale factors while maintaining aspect ratio
        val scaleFactor = min(
            size.width / imageSize.width.toFloat(),
            size.height / imageSize.height.toFloat()
        )

        // Calculate offset to center the scaled image
        val offsetX = (size.width - imageSize.width * scaleFactor) / 2
        val offsetY = (size.height - imageSize.height * scaleFactor) / 2

        detectedObjects.forEach { detectedObject ->
            val boundingBox = detectedObject.boundingBox

            // Scale and position the bounding box
            val left = boundingBox.left * scaleFactor + offsetX
            val top = boundingBox.top * scaleFactor + offsetY
            val right = boundingBox.right * scaleFactor + offsetX
            val bottom = boundingBox.bottom * scaleFactor + offsetY

            // Draw the bounding box
            drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = Dimens.DrawLineStrokeWidth)
            )

            // Prepare label text
            val labelText = detectedObject.labels.firstOrNull()?.text ?: Constants.NOT_APPLICABLE
            val confidence = detectedObject.labels.firstOrNull()?.confidence?.let {
                "%.1f%%".format(it * 100)
            } ?: Constants.NOT_APPLICABLE
            val fullText = "$labelText ($confidence)"

            // Create text paint
            val textPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = Dimens.DrawLabelTextSize
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }

            // Create background paint
            val bgPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Calculate text position (just above the bounding box)
            val textX = left
            val textY = top - 10f // 10 pixels above the box

            // Measure text width
            val textWidth = textPaint.measureText(fullText)

            // Draw text background (slightly larger than text)
            drawContext.canvas.nativeCanvas.drawRect(
                textX - 5f,
                textY - 50f, // Adjust based on text size
                textX + textWidth + 5f,
                textY + 5f,
                bgPaint
            )

            // Draw the text
            drawContext.canvas.nativeCanvas.drawText(
                fullText,
                textX,
                textY,
                textPaint
            )
        }
    }
}