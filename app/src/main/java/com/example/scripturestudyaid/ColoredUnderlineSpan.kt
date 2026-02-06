package com.example.scripturestudyaid

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class ColoredUnderlineSpan(
    private val color: Int,
    private val underlineThickness: Float = 2f
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val originalColor = paint.color
        val width = paint.measureText(text, start, end)

        // Draw Text
        paint.color = originalColor // Ensure text is black/original
        canvas.drawText(text, start, end, x, y.toFloat(), paint)

        // Draw Underline
        paint.color = color
        paint.strokeWidth = underlineThickness
        val lineY = y + 2f + underlineThickness// Slightly below text
        canvas.drawLine(x, lineY, x + width, lineY, paint)

        paint.color = originalColor // Restore
    }
}
