package com.example.scripturestudyaid

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyWindowInsets()
    }

    override fun setContentView(view: android.view.View?) {
        super.setContentView(view)
        applyWindowInsets()
    }

    override fun setContentView(view: android.view.View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyWindowInsets()
    }

    private fun applyWindowInsets() {
        val rootView = findViewById<android.view.View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    protected fun applyTheme() {
        val prefs: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val bgColor = prefs.getString("bgColor", "System")
        val textColor = prefs.getString("textColor", "System")

        val rootView = findViewById<ViewGroup>(android.R.id.content)

        // Apply Background Color
        when (bgColor) {
            "White" -> rootView.setBackgroundColor(Color.WHITE)
            "Black" -> rootView.setBackgroundColor(Color.BLACK)
            "Dark Blue" -> rootView.setBackgroundColor(Color.parseColor("#1a237e")) // Dark blue
            else -> {
                // System default
                 rootView.setBackgroundColor(Color.WHITE) 
            }
        }

        // Apply Text Color recursively
        applyTextColor(rootView, textColor)
    }

    private fun applyTextColor(view: android.view.View, colorName: String?) {
        if (view is TextView) {
            when (colorName) {
                "Black" -> view.setTextColor(Color.BLACK)
                "White" -> view.setTextColor(Color.WHITE)
                "Gold" -> view.setTextColor(Color.parseColor("#b5884f"))
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTextColor(view.getChildAt(i), colorName)
            }
        }
    }
}
