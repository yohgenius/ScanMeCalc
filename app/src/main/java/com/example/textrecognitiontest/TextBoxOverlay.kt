package com.example.textrecognitiontest

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.text.Text
import kotlin.math.ceil

class TextBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val blockRectPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textRectPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var textBlocks: List<Text.TextBlock> = arrayListOf()
    private var imageRectWidth: Float = 0f
    private var imageRectHeight: Float = 0f
    private var isBackCam: Boolean = true

    fun setTextBlocks(blocks: List<Text.TextBlock>, imageRectWidth: Float, imageRectHeight: Float, isBackCam: Boolean) {
        this.textBlocks = blocks
        this.imageRectWidth = imageRectWidth
        this.imageRectHeight = imageRectHeight
        this.isBackCam = isBackCam

        invalidate()
    }

    private fun getBoxRect(imageRectWidth: Float, imageRectHeight: Float, faceBoundingBox: Rect, isBackCam: Boolean): RectF {
        val scaleX = width.toFloat() / imageRectHeight
        val scaleY = height.toFloat() / imageRectWidth
        val scale = scaleX.coerceAtLeast(scaleY)

        val offsetX = (width.toFloat() - ceil(imageRectHeight * scale)) / 2.0f
        val offsetY = (height.toFloat() - ceil(imageRectWidth * scale)) / 2.0f

        val mappedBox = RectF().apply {
            left = faceBoundingBox.right * scale + offsetX
            top = faceBoundingBox.top * scale + offsetY
            right = faceBoundingBox.left * scale + offsetX
            bottom = faceBoundingBox.bottom * scale + offsetY
        }

        val centerX = width.toFloat() / 2

        return if (isBackCam) {
            mappedBox
        } else {
            mappedBox
                .apply {
                    left = centerX + (centerX - left)
                    right = centerX - (right - centerX)
                }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (block in textBlocks) {
            val rect = getBoxRect(imageRectWidth, imageRectHeight, block.boundingBox!!, isBackCam)
            canvas.drawRect(rect, blockRectPaint)

            for (line in block.lines) {
                for (element in line.elements) {
                    val eRect = getBoxRect(imageRectWidth, imageRectHeight, element.boundingBox!!, isBackCam)
                    canvas.drawRect(eRect, textRectPaint)
                }
            }
        }
    }
}