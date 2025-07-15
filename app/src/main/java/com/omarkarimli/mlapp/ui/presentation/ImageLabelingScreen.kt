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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.components.ActionImage
import com.omarkarimli.mlapp.ui.presentation.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Data class to hold an ImageLabel and its associated image URI (if from a picked image)
data class ImageLabelResult(
    val label: ImageLabel,
    val imageUri: Uri? = null // Nullable for live scans
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLabelingScreen(navController: NavHostController) {
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
                Toast.makeText(context, "Camera permission is required for live labeling.", Toast.LENGTH_SHORT).show()
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
        // Only request storage permission if we don't have it already
        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    val imageLabelResults = remember { mutableStateListOf<ImageLabelResult>() }

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
                    analyzeImageForLabels(context, it) { results ->
                        // Clear existing live scan results when a new image is picked
                        imageLabelResults.removeAll { imageLabelResult ->  imageLabelResult.imageUri == null }

                        results.forEach { newImageLabel ->
                            // Avoid adding duplicate labels for the same image (can refine comparison if needed)
                            // This check is more robust, also considering the label itself
                            if (imageLabelResults.none { prevLabel ->
                                    prevLabel.label.text == newImageLabel.label.text && prevLabel.imageUri == newImageLabel.imageUri
                                }) {
                                imageLabelResults.add(newImageLabel)
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
                        Screen.ImageLabeling.title,
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
                                // Request permission if not granted
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
                        onClick = { /* TODO: Implement save functionality */
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
                BottomSheetContentImageLabel(
                    imageLabelResults = imageLabelResults,
                    context = context,
                    onFlipCamera = {
                        // Toggle camera selector
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            Log.e("BarcodeScreen", "Switching to front camera")
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            Log.e("BarcodeScreen", "Switching to back camera")
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
                        CameraPreview(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = Dimens.PaddingMedium)
                                .background(Color.Black, RoundedCornerShape(Dimens.CornerRadiusMedium)),
                            cameraSelector = cameraSelector,
                            onLabelsDetected = { labels ->
                                // Filter out live scan results, then add new ones
                                val currentLiveLabels = imageLabelResults.filter { it.imageUri == null }.toSet()
                                val newLiveLabels = labels.map { ImageLabelResult(it, null) }.toSet()

                                // Only update if there are new labels or existing ones have changed
                                if (newLiveLabels != currentLiveLabels) {
                                    imageLabelResults.removeAll { it.imageUri == null } // Remove old live scan results
                                    imageLabelResults.addAll(newLiveLabels) // Add new ones

                                    coroutineScope.launch {
                                        if (labels.isNotEmpty()) {
                                            // Only partially expand if the sheet is currently hidden or collapsed
                                            if (sheetScaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                                                sheetScaffoldState.bottomSheetState.partialExpand()
                                            }
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

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onLabelsDetected: (List<ImageLabel>) -> Unit,
    cameraSelector: CameraSelector
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    // Get a Future, not an instance directly, as it's an asynchronous operation
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            // Get the camera provider once it's available
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
                        it.setAnalyzer(cameraExecutor, ImageLabelAnalyzer { labels ->
                            onLabelsDetected(labels)
                        })
                    }

                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("ImageLabeler", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx)) // Use main executor for adding listener

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
                    it.setAnalyzer(cameraExecutor, ImageLabelAnalyzer { imageLabels ->
                        onLabelsDetected(imageLabels)
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

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContentImageLabel(imageLabelResults: List<ImageLabelResult>, context: Context, onFlipCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingMedium)
            .background(MaterialTheme.colorScheme.background), // Ensure background for drag handle visibility
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag handle is usually provided by sheetDragHandle in BottomSheetScaffold
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Detected Labels",
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

        if (imageLabelResults.isEmpty()) {
            Text(
                "No labels detected yet. Scan live or pick an image.",
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
                    .heightIn(max = Dimens.BarcodeListMaxHeight), // Important for controlling max height
                contentPadding = PaddingValues(vertical = Dimens.PaddingExtraSmall)
            ) {
                itemsIndexed(imageLabelResults) { index, imageLabelResult ->
                    ImageLabelResultCard(imageLabelResult = imageLabelResult, context = context)
                    if (index < imageLabelResults.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingMedium))
                }
            }
        }
    }
}

class ImageLabelAnalyzer(private val listener: (List<ImageLabel>) -> Unit) : ImageAnalysis.Analyzer {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    // Only invoke listener if labels are non-empty to avoid unnecessary recompositions
                    if (labels.isNotEmpty()) {
                        listener(labels)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ImageLabeler", "Image labeling failed: ${e.message}", e)
                }
                .addOnCompleteListener {
                    imageProxy.close() // Always close the imageProxy
                }
        } else {
            imageProxy.close() // Always close the imageProxy even if mediaImage is null
        }
    }
}

suspend fun analyzeImageForLabels(
    context: Context,
    imageUri: Uri,
    onResult: (List<ImageLabelResult>) -> Unit
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
                Log.e("ImageLabeler", "Error decoding image: ${e.message}", e)
                null
            }

            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)

                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        val imageLabelResults = labels.map { ImageLabelResult(it, imageUri) }
                        onResult(imageLabelResults)
                        if (labels.isEmpty()) {
                            Toast.makeText(context, "No labels found in the selected image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ImageLabeler", "Image labeling failed: ${e.message}", e)
                        Toast.makeText(context, "Error analyzing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to decode image from gallery.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ImageLabeler", "Unexpected error during image analysis: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Unexpected error during image analysis: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun ImageLabelResultCard(imageLabelResult: ImageLabelResult, context: Context) {
    val label = imageLabelResult.label
    val imageUri = imageLabelResult.imageUri

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.PaddingExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Label: ${label.text}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Confidence: ${"%.2f".format(label.confidence)}", style = MaterialTheme.typography.bodyMedium)
        }

        ActionImage(context, imageUri)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ImageLabelingScreenPreview() {
    MLAppTheme {
        ImageLabelingScreen(navController = NavHostController(LocalContext.current))
    }
}