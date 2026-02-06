package com.example.scripturestudyaid

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CreationComparisonActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creation_comparison)

        val comparisonData = JsonUtils.getCreationComparison(this) ?: return

        val rvCreationComparison = findViewById<RecyclerView>(R.id.rvCreationComparison)
        rvCreationComparison.layoutManager = LinearLayoutManager(this)
        rvCreationComparison.adapter = CreationAdapter(comparisonData)
    }
}
