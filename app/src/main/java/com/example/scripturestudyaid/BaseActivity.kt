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
                // System default - usually determined by theme, might need resetting if changing back
                // For now, we won't force reset to system drawable to avoid complexity, 
                // but setting background to null or a default color logic could go here.
                // If we assume light theme is default:
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
                else -> {
                     // Keep default or system text color
                     // To strictly enforce system default we'd need to invalidate or re-inflate, 
                     // but for this simple implementation we might just leave it 
                     // OR force black/white based on presumed background.
                     // For now, do nothing if "System" to let layout XML decide.
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTextColor(view.getChildAt(i), colorName)
            }
        }
    }
}
