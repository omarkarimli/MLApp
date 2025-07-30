package com.omarkarimli.mlapp.ui.presentation.screen.facemeshdetection

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.omarkarimli.mlapp.utils.Dimens
import com.omarkarimli.mlapp.utils.getDeviceScreenRatioDp
import kotlin.math.min

class GraphicOverlayFaceMesh @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

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

    abstract class Graphic(val overlay: GraphicOverlayFaceMesh) {
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

        val ratioScale = context.getDeviceScreenRatioDp() / Dimens.staticDeviceRatio

        translateX = (overlayWidth - imageWidth * scaleFactor) / 2 + (Dimens.FixedFaceMeshPaddingX * ratioScale)
        translateY = (overlayHeight - imageHeight * scaleFactor) / 2 - (Dimens.FixedFaceMeshPaddingY * ratioScale)

        postInvalidate() // Request a redraw with new scale/translation
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (graphic in graphics) {
            graphic.draw(canvas)
        }
    }
}
