package com.omarkarimli.mlapp.ui.presentation.textrecognition

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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.presentation.components.DetectedActionImage
import com.omarkarimli.mlapp.utils.Dimens
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextRecognitionScreen(navController: NavHostController) {

    val viewModel: TextRecognitionViewModel = viewModel()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Get the ImageAnalysis.Analyzer from the ViewModel
    // This analyzer will directly call viewModel.onLiveTextDetected
    val imageAnalyzer = remember { viewModel.createImageAnalyzer() }
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val textResults by viewModel.textResults.collectAsState()
    val cameraSelector by viewModel.cameraSelector.collectAsState()

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
            uri?.let {
                // If an image was picked and results exist, expand the sheet
                if (textResults.any { it.imageUri == uri }) {
                    coroutineScope.launch {
                        if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                            sheetScaffoldState.bottomSheetState.expand()
                        }
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
                        onClick = { viewModel.onSaveClicked() },
                        modifier = Modifier.width(Dimens.IconSizeExtraLarge)
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
                TextRecognitionBottomSheetContent(
                    textResults = textResults,
                    context = context,
                    onFlipCamera = { viewModel.onFlipCamera() }
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
                            analyzer = imageAnalyzer // Pass the analyzer directly
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
private fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer, // Now accepts the analyzer directly
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
            // Add listener to cameraProviderFuture to bind camera once available
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                // Use the analyzer passed from the ViewModel
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, analyzer)
                    }

                try {
                    cameraProvider.unbindAll() // Unbind any previous use cases
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("TextRecognizer", "Use case binding failed", exc)
                    Toast.makeText(context, "Error binding camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(ctx)) // Use main executor for the listener
            previewView
        },
        update = { previewView -> // This block runs on recomposition when cameraSelector changes
            val cameraProvider = cameraProviderFuture.get()

            // Unbind all use cases before rebinding to apply new cameraSelector
            cameraProvider.unbindAll()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // Use the analyzer passed from the ViewModel
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyzer)
                }

            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector, // Use the updated cameraSelector
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("BarcodeScanner", "Error switching camera use case binding", exc)
                Toast.makeText(context, "Error switching camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Ensure cameraExecutor is shut down when the Composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
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
                onClick = onFlipCamera,
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
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium)) // Added spacer for better layout if empty
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.BarcodeListMaxHeight),
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