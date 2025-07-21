package com.omarkarimli.mlapp.ui.presentation

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel // Only default viewModel() import needed
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.components.DetectedActionImage
import com.omarkarimli.mlapp.ui.presentation.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLabelingScreen(navController: NavHostController) {
    val context = LocalContext.current

    val viewModel: ImageLabelingViewModel = viewModel()

    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val imageLabelResults by viewModel.imageLabelResults.collectAsState()
    val cameraSelector by viewModel.cameraSelector.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionResult
    )

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onStoragePermissionResult
    )

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        // CHANGE: Pass context to analyzeImageFromUri
        onResult = { uri: Uri? -> viewModel.analyzeImageFromUri(context, uri) }
    )

    LaunchedEffect(Unit) {
        // CHANGE: Call initializePermissions with context
        viewModel.initializePermissions(context)

        // CHANGE: Call requestPermissions with context
        viewModel.requestPermissions(context, cameraPermissionLauncher, storagePermissionLauncher)

        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // Effect to expand sheet if labels are detected
    LaunchedEffect(imageLabelResults) {
        if (imageLabelResults.isNotEmpty()) {
            if (sheetScaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                sheetScaffoldState.bottomSheetState.partialExpand()
            }
        }
    }

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
                                pickImageLauncher.launch("image/*")
                            } else {
                                // Request permission if not granted
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                Toast.makeText(context, "Storage permission is required to pick photos.", Toast.LENGTH_SHORT).show()
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
                        onClick = viewModel::onSaveClicked,
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
                    onFlipCamera = viewModel::onFlipCameraClicked
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
                            onLabelsDetected = viewModel::onLiveLabelsDetected
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
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = modifier,
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
                        it.setAnalyzer(cameraExecutor, ImageLabelAnalyzer { labels ->
                            onLabelsDetected(labels)
                        })
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector, // Use the provided cameraSelector
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("ImageLabeler", "Use case binding failed", exc)
                    // Toast.makeText(context, "Error setting up camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { previewView ->
            val cameraProvider = cameraProviderFuture.get()

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
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                onClick = onFlipCamera,
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
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.BarcodeListMaxHeight),
                contentPadding = PaddingValues(vertical = Dimens.PaddingExtraSmall)
            ) {
                itemsIndexed(imageLabelResults) { index, imageLabelResult ->
                    ImageLabelResultCard(imageLabelResult = imageLabelResult, context = context)
                    if (index < imageLabelResults.lastIndex) HorizontalDivider()
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
                    if (labels.isNotEmpty()) {
                        listener(labels)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ImageLabeler", "Image labeling failed: ${e.message}", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
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
            .padding(Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Label: ${label.text}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Confidence: ${"%.2f".format(label.confidence)}", style = MaterialTheme.typography.bodyMedium)
        }

        DetectedActionImage(context, imageUri)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ImageLabelingScreenPreview() {
    MLAppTheme {
        ImageLabelingScreen(navController = NavHostController(LocalContext.current))
    }
}