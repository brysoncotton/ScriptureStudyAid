package com.example.scripturestudyaid

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class ComparisonSelectionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparison_selection)

        val btnCreationAccounts = findViewById<Button>(R.id.btnCreationAccounts)
        val btnCallingOfNoah = findViewById<Button>(R.id.btnCallingOfNoah)

        btnCreationAccounts.setOnClickListener {
            val config = ComparisonConfig(
                title = "Creation Accounts",
                filename = "creationAccountsComparison.json",
                source1Name = "Genesis",
                source2Name = "Moses"
            )
            val intent = Intent(this, CreationComparisonActivity::class.java)
            intent.putExtra("config", config)
            startActivity(intent)
        }

        btnCallingOfNoah.setOnClickListener {
            val config = ComparisonConfig(
                title = "The Calling of Noah",
                filename = "ScriptureSideBySide-TheCallingOfNoah.json",
                source1Name = "Genesis",
                source2Name = "Moses"
            )
            val intent = Intent(this, CreationComparisonActivity::class.java)
            intent.putExtra("config", config)
            startActivity(intent)
        }
    }
}
