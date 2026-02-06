package com.example.scripturestudyaid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    var onColorSelected: ((Int) -> Unit)? = null
    var selectedColor: Int = Color.RED
        private set

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(centerX, centerY) * 0.9f
        
        // Cache the wheel bitmap for performance
        updateColorWheelBitmap()
    }

    private lateinit var colorWheelBitmap: Bitmap

    private fun updateColorWheelBitmap() {
        if (radius <= 0) return
        
        val intRadius = radius.toInt()
        if (intRadius <= 0) return

        val size = intRadius * 2
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - intRadius
                val dy = y - intRadius
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                
                if (dist <= intRadius) {
                    val angle = (atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
                    val hue = if (angle < 0) angle + 360 else angle
                    val saturation = dist / intRadius
                    
                    val color = Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
                    bitmap.setPixel(x, y, color)
                }
            }
        }
        colorWheelBitmap = bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (::colorWheelBitmap.isInitialized) {
            canvas.drawBitmap(colorWheelBitmap, centerX - radius, centerY - radius, colorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = sqrt((dx * dx + dy * dy).toDouble())
                
                if (dist <= radius) {
                    val angle = (atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
                    val hue = if (angle < 0) angle + 360 else angle
                    val saturation = (dist / radius).toFloat()
                    
                    selectedColor = Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
                    onColorSelected?.invoke(selectedColor)
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
