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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.material3.BottomSheetScaffoldState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.components.DetectedActionImage
import com.omarkarimli.mlapp.ui.presentation.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Data class to hold recognized text and its associated image URI (if from a picked image)
data class RecognizedText(
    val text: String,
    val imageUri: Uri? = null, // Nullable for live scans
    val boundingBox: android.graphics.Rect? = null // Optional bounding box for display in sheet
)

// Data class to hold detected text and its bounding box for the graphic overlay
data class TextGraphic(
    val text: String,
    val boundingBox: android.graphics.Rect // Use android.graphics.Rect for consistency with ML Kit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextRecognitionScreen(navController: NavHostController) {
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
                Toast.makeText(context, "Camera permission is required for live scanning.", Toast.LENGTH_SHORT).show()
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

    val textResults = remember { mutableStateListOf<RecognizedText>() } // For the bottom sheet
    val detectedGraphics = remember { mutableStateListOf<TextGraphic>() } // For the overlay
    var currentImageWidth by remember { mutableIntStateOf(0) } // State for camera frame width
    var currentImageHeight by remember { mutableIntStateOf(0) } // State for camera frame height


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
                    analyzeImageForText(context, it) { results ->
                        results.forEach { newRecognizedText ->
                            if (textResults.none { prevRecognizedText ->
                                    prevRecognizedText.text == newRecognizedText.text
                                }) {
                                textResults.add(newRecognizedText)
                            }
                        }
                        if (results.isNotEmpty()) {
                            coroutineScope.launch {
                                sheetScaffoldState.bottomSheetState.expand()
                            }
                        }
                        // Clear live scan graphics when picking an image
                        detectedGraphics.clear()
                    }
                }
            }
        }
    )

    // State to hold the current camera selector (front or back)
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    Scaffold(
        topBar = {
            MyTopAppBar(
                navController = navController,
                sheetScaffoldState = sheetScaffoldState,
                hasStoragePermission = hasStoragePermission,
                storagePermissionLauncher = storagePermissionLauncher,
                pickImageLauncher = pickImageLauncher,
                coroutineScope = coroutineScope
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
                TextRecognitionBottomSheetContent(
                    textResults = textResults,
                    context = context,
                    onFlipCamera = {
                        // Toggle camera selector
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            Log.d("TextRecognitionScreen", "Switching to front camera")
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            Log.d("TextRecognitionScreen", "Switching to back camera")
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        detectedGraphics.clear() // Clear graphics on camera flip
                        currentImageWidth = 0 // Reset dimensions to trigger re-calculation
                        currentImageHeight = 0
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
                        Log.d("TextRecognitionScreen", "Camera permission granted. Displaying camera preview.")
                        // Use a Box to layer the camera preview and the overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black, RoundedCornerShape(Dimens.CornerRadiusMedium))
                        ) {
                            CameraPreviewTextRecognition(
                                modifier = Modifier.fillMaxSize(), // Fill the Box
                                cameraSelector = cameraSelector,
                                onTextGraphicsDetected = { graphics, imgWidth, imgHeight -> // Receive dimensions
                                    // Update the list for the overlay
                                    detectedGraphics.clear() // Clear previous frame's graphics
                                    detectedGraphics.addAll(graphics)
                                    currentImageWidth = imgWidth
                                    currentImageHeight = imgHeight

                                    // You can still update textResults for the bottom sheet
                                    // based on the primary recognized text from the first block or line
                                    val newRecognizedText = graphics.firstOrNull()?.text ?: ""
                                    if (newRecognizedText.isNotEmpty()) {
                                        if (textResults.none { it.text == newRecognizedText }) {
                                            textResults.add(RecognizedText(newRecognizedText, null, graphics.firstOrNull()?.boundingBox)) // Add bounding box if needed in sheet
                                        }
                                    }

                                    coroutineScope.launch {
                                        if (newRecognizedText.isNotEmpty()) {
                                            sheetScaffoldState.bottomSheetState.partialExpand()
                                        }
                                    }
                                }
                            )

                            // Add the Graphic Overlay on top of the Camera Preview
                            // Only show overlay if dimensions are valid (avoid division by zero or bad scaling)
                            if (currentImageWidth > 0 && currentImageHeight > 0) {
                                TextGraphicOverlay(
                                    modifier = Modifier.fillMaxSize(),
                                    detectedTextGraphics = detectedGraphics,
                                    imageWidth = currentImageWidth,
                                    imageHeight = currentImageHeight
                                )
                            }
                        }
                    } else {
                        Log.d("TextRecognitionScreen", "Camera permission not granted. Showing placeholder.")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(
    navController: NavHostController,
    sheetScaffoldState: BottomSheetScaffoldState,
    hasStoragePermission: Boolean,
    storagePermissionLauncher: ActivityResultLauncher<String>,
    pickImageLauncher: ActivityResultLauncher<String>,
    coroutineScope: CoroutineScope
) {
    return TopAppBar(
        title = {
            Text(
                Screen.TextRecognition.title,
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
                        coroutineScope.launch { sheetScaffoldState.bottomSheetState.partialExpand() }
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
                onClick = { /* doSomething() */ }, // Implement save functionality
                modifier = Modifier.width(Dimens.IconSizeExtraLarge).height(Dimens.IconSizeLarge),
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

@Composable
private fun CameraPreviewTextRecognition(
    modifier: Modifier = Modifier,
    onTextGraphicsDetected: (List<TextGraphic>, Int, Int) -> Unit, // Updated signature
    cameraSelector: CameraSelector // Receive camera selector as a parameter
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Log.d("CameraPreviewTR", "Factory: Creating PreviewView")
            val previewView = PreviewView(ctx).apply {
                this.scaleType = PreviewView.ScaleType.FIT_CENTER
            }
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
                    it.setAnalyzer(cameraExecutor, TextAnalyzer { recognizedTextGraphics, imgWidth, imgHeight ->
                        onTextGraphicsDetected(recognizedTextGraphics, imgWidth, imgHeight)
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector, // Use the provided cameraSelector initially
                    preview,
                    imageAnalyzer
                )
                //Log.d("CameraPreviewTR", "Factory: Camera bound successfully with selector: ${cameraSelector.lensFacing.toString()}")
            } catch (exc: Exception) {
                Log.e("CameraPreviewTR", "Factory: Use case binding failed", exc)
            }
            previewView
        },
        update = { previewView -> // This block runs on recomposition when cameraSelector changes
            //Log.d("CameraPreviewTR", "Update: Recomposition detected for CameraPreviewTextRecognition. Camera Selector: ${cameraSelector.lensFacing}")
            val cameraProvider = cameraProviderFuture.get()

            // Unbind all use cases before rebinding to apply new camera selector
            cameraProvider.unbindAll()
            Log.d("CameraPreviewTR", "Update: Unbound all use cases.")


            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, TextAnalyzer { detectedTexts, imgWidth, imgHeight ->
                        onTextGraphicsDetected(detectedTexts, imgWidth, imgHeight)
                    })
                }

            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector, // Use the updated cameraSelector
                    preview,
                    imageAnalyzer
                )
                // Log.d("CameraPreviewTR", "Update: Camera rebound successfully with selector: ${cameraSelector.lensFacing}")
            } catch (exc: Exception) {
                Log.e("CameraPreviewTR", "Update: Use case binding failed: ${exc.message}", exc)
                Toast.makeText(context, "Error switching camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            Log.d("CameraPreviewTR", "DisposableEffect: Shutting down camera executor.")
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun TextGraphicOverlay(
    modifier: Modifier = Modifier,
    detectedTextGraphics: List<TextGraphic>,
    imageWidth: Int, // The width of the image being analyzed (e.g., camera frame width)
    imageHeight: Int // The height of the image being analyzed (e.g., camera frame height)
) {
    Canvas(modifier = modifier) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            Log.w("TextGraphicOverlay", "Invalid image dimensions: $imageWidth x $imageHeight. Skipping drawing.")
            return@Canvas
        }

        // Calculate scaling factors relative to the Canvas's current size
        // size.width and size.height refer to the dimensions of the Composable itself
        val scaleX = size.width / imageWidth
        val scaleY = size.height / imageHeight

        detectedTextGraphics.forEach { graphic ->
            val rect = graphic.boundingBox

            // Scale and translate the bounding box to fit the Canvas composable's dimensions
            val scaledRect = rect.toComposeRect().let {
                it.copy(
                    left = it.left * scaleX,
                    top = it.top * scaleY,
                    right = it.right * scaleX,
                    bottom = it.bottom * scaleY
                )
            }

            // Draw the rectangle
            drawRoundRect(
                color = Color.White,
                topLeft = scaledRect.topLeft,
                size = scaledRect.size,
                cornerRadius = CornerRadius(x = 8.dp.toPx(), y = 8.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextRecognitionBottomSheetContent(textResults: List<RecognizedText>, context: Context, onFlipCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recognized Text",
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

        if (textResults.isEmpty()) {
            Text(
                "No text recognized yet. Scan live or pick an image.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingSmall),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.BarcodeListMaxHeight), // Reusing Dimens.BarcodeListMaxHeight for consistency
                contentPadding = PaddingValues(vertical = Dimens.PaddingExtraSmall)
            ) {
                itemsIndexed(textResults) { index, recognizedText ->
                    TextResultCard(recognizedText = recognizedText, context = context)
                    if (index < textResults.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

private class TextAnalyzer(private val listener: (List<TextGraphic>, Int, Int) -> Unit) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val frameWidth = imageProxy.width // Get actual width from ImageProxy
            val frameHeight = imageProxy.height // Get actual height from ImageProxy

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedTextGraphics = mutableListOf<TextGraphic>()
                    for (block in visionText.textBlocks) {
                        // For simplicity, we'll draw rectangles for each TextBlock
                        // You could iterate further (lines, elements) for more granular boxes
                        block.boundingBox?.let { bbox ->
                            detectedTextGraphics.add(TextGraphic(block.text, bbox))
                        }
                    }
                    listener(detectedTextGraphics, frameWidth, frameHeight)
                }
                .addOnFailureListener { e ->
                    Log.e("TextAnalyzer", "Text recognition failed: ${e.message}", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

private suspend fun analyzeImageForText(
    context: Context,
    imageUri: Uri,
    onResult: (List<RecognizedText>) -> Unit
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
                Log.e("TextRecognizer", "Error decoding image: ${e.message}", e)
                null
            }

            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = visionText.text
                        val results = if (recognizedText.isNotEmpty()) {
                            // For picked images, we typically don't have bounding boxes directly for individual blocks
                            // or it's not often used in the same way as live camera.
                            // If you need it, you would iterate visionText.textBlocks here.
                            listOf(RecognizedText(recognizedText, imageUri, visionText.textBlocks.firstOrNull()?.boundingBox))
                        } else {
                            emptyList()
                        }
                        onResult(results)
                        if (recognizedText.isEmpty()) {
                            Toast.makeText(context, "No text found in the selected image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("TextRecognizer", "Image text recognition failed: ${e.message}", e)
                        Toast.makeText(context, "Error analyzing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to decode image from gallery.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("TextRecognizer", "Unexpected error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun TextResultCard(recognizedText: RecognizedText, context: Context) {
    val imageUri = recognizedText.imageUri

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Text:", style = MaterialTheme.typography.bodyLarge)
            Text(text = recognizedText.text, style = MaterialTheme.typography.bodyMedium)
        }

        DetectedActionImage(context, imageUri)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TextRecognitionScreenPreview() {
    MLAppTheme {
        TextRecognitionScreen(navController = NavHostController(LocalContext.current))
    }
}