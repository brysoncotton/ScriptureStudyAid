package com.example.scripturestudyaid

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class ComparisonSelectionActivity : BaseActivity() {
    private lateinit var adapter: CustomComparisonListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparison_selection)

        val btnCreationAccounts = findViewById<Button>(R.id.btnCreationAccounts)
        val btnCallingOfNoah = findViewById<Button>(R.id.btnCallingOfNoah)
        val btnCreateNew = findViewById<Button>(R.id.btnCreateNew)
        val rvCustomComparisons = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCustomComparisons)

        // Setup Standard Comparisons
        btnCreationAccounts.setOnClickListener {
            launchComparison("Creation Accounts", "creationAccountsComparison.json", "Genesis", "Moses")
        }

        btnCallingOfNoah.setOnClickListener {
            launchComparison("The Calling of Noah", "ScriptureSideBySide-TheCallingOfNoah.json", "Genesis", "Moses")
        }

        // Setup Custom Comparisons
        rvCustomComparisons.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter = CustomComparisonListAdapter(emptyList(), 
            onItemClick = { comparison ->
                // Launch Viewer for Custom Comparison
                // For custom comparisons, we need to pass the ID or the object itself
                // Since the viewer expects a filename for standard ones, we might need to adjust logic
                // For now, let's assume we pass the ID and the viewer loads it
                val intent = Intent(this, CreationComparisonActivity::class.java)
                val config = ComparisonConfig(
                     title = comparison.title,
                     filename = "", // Empty filename indicates custom? Or we use a special prefix?
                     source1Name = "Left", // Dynamic?
                     source2Name = "Right",
                     customComparisonId = comparison.id // Need to add this to ComparisonConfig
                )
                intent.putExtra("config", config)
                startActivity(intent)
            },
            onItemLongClick = { comparison ->
                showDeleteDialog(comparison)
            }
        )
        rvCustomComparisons.adapter = adapter

        btnCreateNew.setOnClickListener {
            val intent = Intent(this, CustomComparisonEditorActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadCustomComparisons()
    }

    private fun loadCustomComparisons() {
        val comparisons = CustomComparisonRepository.getComparisons(this)
        adapter.updateList(comparisons)
    }

    private fun launchComparison(title: String, filename: String, s1: String, s2: String) {
        val config = ComparisonConfig(title, filename, s1, s2)
        val intent = Intent(this, CreationComparisonActivity::class.java)
        intent.putExtra("config", config)
        startActivity(intent)
    }

    private fun showDeleteDialog(comparison: CustomComparison) {
         androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Comparison")
            .setMessage("Are you sure you want to delete \"${comparison.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                CustomComparisonRepository.deleteComparison(this, comparison.id)
                loadCustomComparisons()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
