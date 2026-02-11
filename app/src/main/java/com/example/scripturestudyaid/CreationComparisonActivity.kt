package com.example.scripturestudyaid

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView

class CreationComparisonActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creation_comparison)

        val config = intent.getSerializableExtra("config") as? ComparisonConfig
        if (config == null) {
            finish()
            return
        }

        val tvComparisonTitle = findViewById<TextView>(R.id.tvComparisonTitle)
        tvComparisonTitle.text = config.title

        val comparisonData = if (config.customComparisonId != null) {
            val customComparisons = CustomComparisonRepository.getComparisons(this)
            val custom = customComparisons.find { it.id == config.customComparisonId }
            custom?.pairs?.map { pair ->
                ComparisonItem(
                    source1Verse = pair.left.verseReference,
                    source1Text = pair.left.text,
                    source2Verse = pair.right.verseReference,
                    source2Text = pair.right.text
                )
            } ?: emptyList()
        } else {
            JsonUtils.getComparisonData(
                this, 
                config.filename, 
                config.source1Name, 
                config.source2Name
            ) ?: emptyList()
        }
        
        if (comparisonData.isEmpty()) {
            // Handle error or empty state?
            // For now just return, maybe show toast
            return
        }

        val rvCreationComparison = findViewById<RecyclerView>(R.id.rvCreationComparison)
        rvCreationComparison.layoutManager = LinearLayoutManager(this)
        rvCreationComparison.adapter = ComparisonAdapter(comparisonData, config.source1Name, config.source2Name)
    }
}
