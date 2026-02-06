package com.example.scripturestudyaid

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() { // Not extending BaseActivity to avoid theming issues while editing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val rgBg = findViewById<RadioGroup>(R.id.rgBackgroundColor)
        val rgText = findViewById<RadioGroup>(R.id.rgTextColor)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        val prefs: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val currentBg = prefs.getString("bgColor", "System")
        val currentText = prefs.getString("textColor", "System")

        // Set current selection
        when (currentBg) {
            "White" -> rgBg.check(R.id.rbBgWhite)
            "Black" -> rgBg.check(R.id.rbBgBlack)
            "Dark Blue" -> rgBg.check(R.id.rbBgDarkBlue)
            else -> rgBg.check(R.id.rbBgSystem)
        }

        when (currentText) {
            "Black" -> rgText.check(R.id.rbTextBlack)
            "White" -> rgText.check(R.id.rbTextWhite)
            "Gold" -> rgText.check(R.id.rbTextGold)
            else -> rgText.check(R.id.rbTextSystem)
        }

        btnSave.setOnClickListener {
            val editor = prefs.edit()

            val selectedBgId = rgBg.checkedRadioButtonId
            val bgValue = when (selectedBgId) {
                R.id.rbBgWhite -> "White"
                R.id.rbBgBlack -> "Black"
                R.id.rbBgDarkBlue -> "Dark Blue"
                else -> "System"
            }
            editor.putString("bgColor", bgValue)

            val selectedTextId = rgText.checkedRadioButtonId
            val textValue = when (selectedTextId) {
                R.id.rbTextBlack -> "Black"
                R.id.rbTextWhite -> "White"
                R.id.rbTextGold -> "Gold"
                else -> "System"
            }
            editor.putString("textColor", textValue)

            editor.apply()
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
            finish() // Close settings to return to previous screen which should reload theme
        }
    }
}
