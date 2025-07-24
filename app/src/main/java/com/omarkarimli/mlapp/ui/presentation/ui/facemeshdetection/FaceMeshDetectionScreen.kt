package com.omarkarimli.mlapp.ui.presentation.ui.facemeshdetection

import android.Manifest
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as GraphicsColor
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.facemesh.FaceMesh
import com.omarkarimli.mlapp.domain.models.ScannedFaceMesh
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.ui.components.DetectedActionImage
import com.omarkarimli.mlapp.ui.presentation.ui.components.CameraPermissionPlaceholder
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.min

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun FaceMeshDetectionScreenPreview() {
    MLAppTheme {
        FaceMeshDetectionScreen(navController = NavHostController(LocalContext.current))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceMeshDetectionScreen(navController: NavHostController) {

    val viewModel: FaceMeshDetectionViewModel = viewModel()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val faceMeshResults by viewModel.faceMeshResults.collectAsState() // Now collectAsState
    val cameraSelector by viewModel.cameraSelector.collectAsState()
    val detectedFaceMeshesForOverlay by viewModel.detectedFaceMeshesForOverlay.collectAsState()
    val imageSizeForOverlay by viewModel.imageSizeForOverlay.collectAsState()

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
            if (faceMeshResults.any { it.imageUri != null }) { // Check if there are results specifically from picked images
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
                        Screen.FaceMeshDetection.title,
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
                BottomSheetContentFaceMeshes(
                    faceMeshResults = faceMeshResults,
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
                            detectedFaceMeshes = detectedFaceMeshesForOverlay, // Pass data for overlay
                            imageSize = imageSizeForOverlay, // Pass image size for overlay
                            // Pass the ViewModel's analyzeLiveFaceMesh method as a lambda
                            onImageProxyAnalyzed = { imageProxy ->
                                viewModel.analyzeLiveFaceMesh(imageProxy)
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

private class GraphicOverlay @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val graphics = mutableListOf<Graphic>()
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    // Dimensions of the original image/camera frame
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    // Dimensions of the view where graphics are drawn
    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0

    abstract class Graphic(val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)

        // Helper method for scaling and translating points from image coordinates to overlay coordinates
        fun translateX(x: Float): Float = x * overlay.scaleFactor + overlay.translateX
        fun translateY(y: Float): Float = y * overlay.scaleFactor + overlay.translateY
    }

    fun clear() {
        graphics.clear()
        postInvalidate() // Request a redraw
    }

    fun add(graphic: Graphic) {
        graphics.add(graphic)
    }

    fun setImageSourceInfo(imageWidth: Int, imageHeight: Int) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        updateScaleAndTranslation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        overlayWidth = w
        overlayHeight = h
        updateScaleAndTranslation()
    }

    private fun updateScaleAndTranslation() {
        if (imageWidth == 0 || imageHeight == 0 || overlayWidth == 0 || overlayHeight == 0) {
            return
        }

        val scaleX = overlayWidth.toFloat() / imageWidth
        val scaleY = overlayHeight.toFloat() / imageHeight
        scaleFactor = min(scaleX, scaleY) * Dimens.FixedFaceMeshScaleFactor

        translateX = (overlayWidth - imageWidth * scaleFactor) / 2 + Dimens.FixedFaceMeshPaddingX
        translateY = (overlayHeight - imageHeight * scaleFactor) / 2 - Dimens.FixedFaceMeshPaddingY

        postInvalidate() // Request a redraw with new scale/translation
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (graphic in graphics) {
            graphic.draw(canvas)
        }
    }
}

private class FaceMeshGraphic(overlay: GraphicOverlay, private val faceMesh: FaceMesh, private val isFrontCamera: Boolean) : GraphicOverlay.Graphic(overlay) {

    private val meshPaint = Paint().apply {
        color = GraphicsColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        color = GraphicsColor.CYAN
        style = Paint.Style.FILL
        strokeWidth = 4f
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        // For front camera, we need to mirror the X coordinates
        val mirrorX = { x: Float ->
            if (isFrontCamera) overlay.width - translateX(x) else translateX(x)
        }

        // Draw all points
        for (point in faceMesh.allPoints) {
            canvas.drawCircle(
                mirrorX(point.position.x),
                translateY(point.position.y),
                2f,
                pointPaint
            )
        }

        // Draw the mesh triangles
        for (triangle in faceMesh.allTriangles) {
            val p1 = triangle.allPoints[0].position
            val p2 = triangle.allPoints[1].position
            val p3 = triangle.allPoints[2].position

            // Draw lines connecting the triangle points with mirroring for front camera
            canvas.drawLine(
                mirrorX(p1.x), translateY(p1.y),
                mirrorX(p2.x), translateY(p2.y),
                meshPaint
            )
            canvas.drawLine(
                mirrorX(p2.x), translateY(p2.y),
                mirrorX(p3.x), translateY(p3.y),
                meshPaint
            )
            canvas.drawLine(
                mirrorX(p3.x), translateY(p3.y),
                mirrorX(p1.x), translateY(p1.y),
                meshPaint
            )
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    // This is the new parameter: a lambda that takes an ImageProxy
    // and is responsible for its analysis and closure.
    onImageProxyAnalyzed: (ImageProxy) -> Unit,
    cameraSelector: CameraSelector,
    detectedFaceMeshes: List<FaceMesh>, // Data for the overlay from ViewModel
    imageSize: Size // Image size for the overlay from ViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // State for the GraphicOverlay
    val graphicOverlay = remember { GraphicOverlay(context) }

    // Update GraphicOverlay when detectedFaceMeshes or imageSize changes
    LaunchedEffect(detectedFaceMeshes, imageSize, cameraSelector) {
        graphicOverlay.clear()
        graphicOverlay.setImageSourceInfo(imageSize.width, imageSize.height)
        detectedFaceMeshes.forEach { faceMesh ->
            // Pass true for front camera, false for back camera
            graphicOverlay.add(FaceMeshGraphic(graphicOverlay, faceMesh, cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA))
        }
        graphicOverlay.postInvalidate() // Request redraw for the overlay
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FIT_START // Keep FIT_START or adjust as needed
                }
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            // Set the analyzer to call the provided lambda
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                onImageProxyAnalyzed(imageProxy)
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                        Toast.makeText(context, "Error binding camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                val cameraProvider = cameraProviderFuture.get()

                // Unbind all use cases before rebinding, essential for camera switching
                cameraProvider.unbindAll()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        // Re-set the analyzer when camera selector changes
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            onImageProxyAnalyzed(imageProxy)
                        }
                    }

                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector, // Use the updated camera selector
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Error switching camera: ${exc.message}", exc)
                    Toast.makeText(context, "Error switching camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Add the GraphicOverlay on top of the PreviewView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { graphicOverlay }
        )
    }

    // Shut down the camera executor when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContentFaceMeshes(faceMeshResults: List<ScannedFaceMesh>, context: Context, onFlipCamera: () -> Unit) {
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
                text = "Detected Face Meshes",
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

        if (faceMeshResults.isEmpty()) {
            Text(
                "No face meshes detected yet. Scan live or pick an image.",
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
                itemsIndexed(faceMeshResults) { index, scannedFaceMesh ->
                    FaceMeshResultCard(scannedFaceMesh = scannedFaceMesh, context = context)
                    if (index < faceMeshResults.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FaceMeshResultCard(scannedFaceMesh: ScannedFaceMesh, context: Context) {
    val faceMesh = scannedFaceMesh.faceMesh
    val imageUri = scannedFaceMesh.imageUri

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Bounding Box: ${faceMesh.boundingBox}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = "Number of Points: ${faceMesh.allPoints.size}", style = MaterialTheme.typography.bodyLarge)
            // You can add more details about face mesh here, e.g., specific contours
        }

        DetectedActionImage(context, imageUri)
    }
}
