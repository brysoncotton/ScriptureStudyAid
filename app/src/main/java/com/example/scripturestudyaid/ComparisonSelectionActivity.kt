package com.example.scripturestudyaid

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class ComparisonSelectionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparison_selection)

        val btnCreationAccounts = findViewById<Button>(R.id.btnCreationAccounts)

        btnCreationAccounts.setOnClickListener {
            val intent = Intent(this, CreationComparisonActivity::class.java)
            startActivity(intent)
        }
    }
}
