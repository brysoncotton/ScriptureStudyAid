package com.example.scripturestudyaid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FrequencyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint().apply {
        color = Color.parseColor("#4A90E2") // Standard Blue, can be themed
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val axisPaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var multiData: List<Pair<String, Map<String, Int>>> = emptyList()
    private var maxCount = 0
    private var isStacked = true // Default to stacked/sum
    private val seriesColors = listOf(
        Color.parseColor("#4A90E2"), // Blue
        Color.parseColor("#E24A4A"), // Red
        Color.parseColor("#4AE24A"), // Green
        Color.parseColor("#E24AE2"), // Purple
        Color.parseColor("#E2904A")  // Orange
    )

    private var listener: OnBarClickListener? = null

    interface OnBarClickListener {
        fun onBarClick(label: String)
    }

    fun setOnBarClickListener(listener: OnBarClickListener) {
        this.listener = listener
    }

    fun setData(newData: Map<String, Map<String, Int>>, stacked: Boolean = true) {
        multiData = newData.entries.sortedByDescending { it.value.values.sum() }.map { it.toPair() }
        isStacked = stacked
        
        // Calculate max based on mode
        maxCount = if (isStacked) {
            multiData.maxOfOrNull { it.second.values.sum() } ?: 0
        } else {
            multiData.maxOfOrNull { it.second.values.maxOrNull() ?: 0 } ?: 0
        }
        
        requestLayout()
        invalidate()
    }

    // Re-declare constants here if needed, or use class properties if they were preserved. 
    // Wait, I removed them in previous step. I should have kept them or re-added them.
    private val barHeight = 60f
    private val barGap = 30f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val effectiveBarHeight = if (isStacked) barHeight else (barHeight * 1.5f)
        val heightPerItem = effectiveBarHeight + barGap
        
        val desiredHeight = (multiData.size * heightPerItem + 100).toInt()
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> kotlin.math.min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(desiredWidth, if (multiData.isNotEmpty()) desiredHeight else height)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            val y = event.y
            val paddingTop = 50f
            val effectiveBarHeight = if (isStacked) barHeight else (barHeight * 1.5f)
            val heightPerItem = effectiveBarHeight + barGap
            
            if (y > paddingTop) {
                val index = ((y - paddingTop) / heightPerItem).toInt()
                if (index in multiData.indices) {
                    listener?.onBarClick(multiData[index].first)
                    return true
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (multiData.isEmpty() || maxCount == 0) return

        val width = width.toFloat()
        val leftMargin = 300f 
        val rightMargin = 100f
        val chartWidth = width - leftMargin - rightMargin
        
        var y = 50f
        val effectiveBarHeight = if (isStacked) barHeight else (barHeight * 1.5f)
        
        textPaint.textAlign = Paint.Align.RIGHT

        multiData.forEach { (label, counts) ->
            // Draw Label
             val maxLabelWidth = leftMargin - 20
            val words = label.split(" ")
            var line = ""
            val lineHeight = textPaint.textSize + 5
            val lines = mutableListOf<String>()

            words.forEach { word ->
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (textPaint.measureText(testLine) > maxLabelWidth) {
                    lines.add(line)
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) lines.add(line)

            val textBlockHeight = lines.size * lineHeight
            var textY = y + (effectiveBarHeight - textBlockHeight) / 2 + lineHeight - 5

            lines.forEach { 
                canvas.drawText(it, leftMargin - 20, textY, textPaint)
                textY += lineHeight
            }

            // Draw Bars
            if (isStacked) {
                var currentX = leftMargin
                var totalCount = 0
                counts.entries.forEachIndexed { index, entry ->
                    val seriesCount = entry.value
                    if (seriesCount > 0) {
                        val seriesWidth = (seriesCount.toFloat() / maxCount) * chartWidth
                        barPaint.color = seriesColors[index % seriesColors.size]
                        canvas.drawRect(currentX, y, currentX + seriesWidth, y + barHeight, barPaint)
                        currentX += seriesWidth
                        totalCount += seriesCount
                    }
                }
                // Draw Total Count
                val originalAlign = textPaint.textAlign
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(totalCount.toString(), currentX + 20, y + (barHeight / 2) + 10, textPaint)
                textPaint.textAlign = originalAlign
                
            } else {
                // Grouped
                val terms = counts.keys.toList()
                val subBarHeight = effectiveBarHeight / terms.size
                
                terms.forEachIndexed { index, term ->
                    val seriesCount = counts[term] ?: 0
                    if (seriesCount >= 0) {
                         val seriesWidth = (seriesCount.toFloat() / maxCount) * chartWidth
                         barPaint.color = seriesColors[index % seriesColors.size]
                         val top = y + (index * subBarHeight)
                         
                         canvas.drawRect(leftMargin, top, leftMargin + seriesWidth, top + subBarHeight - 2, barPaint)
                         
                         val originalAlign = textPaint.textAlign
                         textPaint.textAlign = Paint.Align.LEFT
                         val originalSize = textPaint.textSize
                         textPaint.textSize = 20f
                         canvas.drawText(seriesCount.toString(), leftMargin + seriesWidth + 10, top + subBarHeight - 5, textPaint)
                         textPaint.textSize = originalSize
                         textPaint.textAlign = originalAlign
                    }
                }
            }

            y += effectiveBarHeight + barGap
        }
        
        // Draw Y-Axis Line
        canvas.drawLine(leftMargin, 20f, leftMargin, y, axisPaint)
    }
}
