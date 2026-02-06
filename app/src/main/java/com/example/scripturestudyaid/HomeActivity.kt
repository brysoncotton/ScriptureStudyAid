package com.example.scripturestudyaid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnReadScriptures = findViewById<Button>(R.id.btnReadScriptures)
        val btnCompare = findViewById<Button>(R.id.btnCompare)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnReadScriptures.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnCompare.setOnClickListener {
            val intent = Intent(this, ComparisonSelectionActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
