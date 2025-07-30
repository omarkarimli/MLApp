package com.omarkarimli.mlapp.ui.presentation.screen.objectdetection

import android.graphics.Paint
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.omarkarimli.mlapp.domain.models.ScannedObject
import com.omarkarimli.mlapp.utils.Constants
import com.omarkarimli.mlapp.utils.Dimens
import kotlin.collections.forEach
import kotlin.math.min

@Composable
fun GraphicOverlayObject(objectResults: List<ScannedObject>, imageSize: Size, modifier: Modifier = Modifier) {
    // Only Live
    val filteredObjectResults = objectResults.filter { it.imageUri == null }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageSize.width <= 0 || imageSize.height <= 0) return@Canvas

        val scaleFactor = min(
            size.width / imageSize.width.toFloat(),
            size.height / imageSize.height.toFloat()
        )

        val offsetX = (size.width - imageSize.width * scaleFactor) / 2
        val offsetY = (size.height - imageSize.height * scaleFactor) / 2

        filteredObjectResults.forEach { objectResult ->
            val detectedObject = objectResult.detectedObject
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