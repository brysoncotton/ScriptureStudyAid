package com.example.scripturestudyaid

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.RangeSlider

class HighlightBottomSheetFragment : BottomSheetDialogFragment() {

    interface OnHighlightOptionSelectedListener {
        fun onHighlightOptionSelected(color: Int, type: String, start: Int, end: Int)
        fun onSliderValueChanged(start: Int, end: Int)
        fun onHighlightDeleted()
    }

    private var listener: OnHighlightOptionSelectedListener? = null
    private var selectedColor: Int = Color.YELLOW
    private var selectedType: String = "SOLID"
    private var initialStart: Int = 0
    private var initialEnd: Int = 0
    private var maxRange: Int = 100
    private var isEditMode: Boolean = false

    fun setListener(listener: OnHighlightOptionSelectedListener) {
        this.listener = listener
    }

    fun setInitialRange(start: Int, end: Int, max: Int) {
        this.initialStart = start
        this.initialEnd = end
        this.maxRange = max
    }

    fun setEditMode(isEdit: Boolean) {
        this.isEditMode = isEdit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_highlight_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rgHighlightType = view.findViewById<RadioGroup>(R.id.rgHighlightType)
        val gridColors = view.findViewById<GridLayout>(R.id.gridColors)
        val colorWheel = view.findViewById<ColorWheelView>(R.id.colorWheel)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val rangeSlider = view.findViewById<RangeSlider>(R.id.rangeSlider)

        btnDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE

        // Configure Slider
        rangeSlider.valueFrom = 0f
        rangeSlider.valueTo = maxRange.toFloat()
        rangeSlider.values = listOf(initialStart.toFloat(), initialEnd.toFloat())
        
        rangeSlider.addOnChangeListener { slider, _, fromUser ->
            if (fromUser) {
                val values = slider.values
                val start = values[0].toInt()
                val end = values[1].toInt()
                listener?.onSliderValueChanged(start, end)
                initialStart = start
                initialEnd = end
            }
        }

        // Type Selection
        rgHighlightType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = if (checkedId == R.id.rbUnderline) "UNDERLINE" else "SOLID"
        }

        // Preset Colors
        val presets = listOf(
            Color.YELLOW, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED,
            Color.BLUE, Color.LTGRAY, Color.parseColor("#FFA500"), // Orange
            Color.parseColor("#800080"), // Purple
            Color.parseColor("#FFC0CB")  // Pink
        )

        val margin = (resources.displayMetrics.density * 8).toInt()
        val size = (resources.displayMetrics.density * 40).toInt()

        presets.forEach { color ->
            val colorView = View(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, Color.LTGRAY)
                }
                setOnClickListener {
                    selectedColor = color
                    colorWheel.invalidate() // Optional: Update wheel selection if supported
                }
            }
            gridColors.addView(colorView)
        }

        // Custom Color
        colorWheel.onColorSelected = { color ->
            selectedColor = color
        }

        // Action Buttons
        btnSave.setOnClickListener {
            listener?.onHighlightOptionSelected(selectedColor, selectedType, initialStart, initialEnd)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnDelete.setOnClickListener {
            listener?.onHighlightDeleted()
            dismiss()
        }
    }

    companion object {
        const val TAG = "HighlightBottomSheet"
    }
}