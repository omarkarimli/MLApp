package com.omarkarimli.mlapp.ui.presentation.ui.barcodescanning

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun BarcodeCameraPreview(modifier: Modifier = Modifier, cameraSelector: CameraSelector, analyzeLiveBarcode: (ImageProxy) -> Unit) {
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
            }
        }
    )

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }
}
