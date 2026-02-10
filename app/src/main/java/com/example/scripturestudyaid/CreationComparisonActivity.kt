package com.example.scripturestudyaid

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CreationComparisonActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creation_comparison)

        val config = intent.getSerializableExtra("config") as? ComparisonConfig
        if (config == null) {
            finish()
            return
        }

        // Set title if you have a toolbar, or just use the config title
        // For now, let's assume we might want to set the title on the toolbar if it exists
        // val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)
        // tvToolbarTitle.text = config.title

        val comparisonData = JsonUtils.getComparisonData(
            this, 
            config.filename, 
            config.source1Name, 
            config.source2Name
        ) ?: return

        val rvCreationComparison = findViewById<RecyclerView>(R.id.rvCreationComparison)
        rvCreationComparison.layoutManager = LinearLayoutManager(this)
        rvCreationComparison.adapter = ComparisonAdapter(comparisonData, config.source1Name, config.source2Name)
    }
}
