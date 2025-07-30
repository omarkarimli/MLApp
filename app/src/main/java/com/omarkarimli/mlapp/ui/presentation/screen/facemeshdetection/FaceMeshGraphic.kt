package com.omarkarimli.mlapp.ui.presentation.screen.facemeshdetection

import android.graphics.Canvas
import android.graphics.Paint
import com.google.mlkit.vision.facemesh.FaceMesh
import android.graphics.Color as GraphicsColor

class FaceMeshGraphic(overlay: GraphicOverlayFaceMesh, private val faceMesh: FaceMesh, private val isFrontCamera: Boolean) : GraphicOverlayFaceMesh.Graphic(overlay) {

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
