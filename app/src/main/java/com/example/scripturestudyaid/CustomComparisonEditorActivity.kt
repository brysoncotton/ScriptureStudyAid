package com.example.scripturestudyaid

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class CustomComparisonEditorActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var rvEditor: RecyclerView
    private lateinit var btnAddRow: Button
    private lateinit var btnSave: Button
    
    private lateinit var adapter: CustomComparisonEditorAdapter
    private val pairs = mutableListOf<CustomComparisonPair>()
    
    private var pendingPosition = -1
    private var pendingIsLeft = true

    private val selectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringExtra("verse_text")
            val ref = result.data?.getStringExtra("verse_ref")
            
            if (text != null && ref != null && pendingPosition != -1) {
                val pair = pairs[pendingPosition]
                if (pendingIsLeft) {
                    pair.left = CustomComparisonItem(ref, text)
                } else {
                    pair.right = CustomComparisonItem(ref, text)
                }
                adapter.notifyItemChanged(pendingPosition)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_comparison_editor)

        etTitle = findViewById(R.id.etTitle)
        rvEditor = findViewById(R.id.rvEditor)
        btnAddRow = findViewById(R.id.btnAddRow)
        btnSave = findViewById(R.id.btnSave)
        
        // Apply window insets to root layout
        val rootLayout = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize with one empty row
        pairs.add(CustomComparisonPair(CustomComparisonItem(), CustomComparisonItem()))

        adapter = CustomComparisonEditorAdapter(
            pairs,
            onAddVerseClick = { position, isLeft ->
                pendingPosition = position
                pendingIsLeft = isLeft
                val intent = Intent(this, VerseSelectorActivity::class.java)
                selectorLauncher.launch(intent)
            },
            onDeleteRowClick = { position ->
                if (pairs.size > 1) {
                    pairs.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    adapter.notifyItemRangeChanged(position, pairs.size)
                } else {
                    Toast.makeText(this, "Cannot delete the only row", Toast.LENGTH_SHORT).show()
                }
            },
            onClearCellClick = { position, isLeft ->
                val pair = pairs[position]
                if (isLeft) {
                    pair.left = CustomComparisonItem()
                } else {
                    pair.right = CustomComparisonItem()
                }
                adapter.notifyItemChanged(position)
            }
        )

        rvEditor.layoutManager = LinearLayoutManager(this)
        rvEditor.adapter = adapter

        btnAddRow.setOnClickListener {
            pairs.add(CustomComparisonPair(CustomComparisonItem(), CustomComparisonItem()))
            adapter.notifyItemInserted(pairs.size - 1)
            rvEditor.scrollToPosition(pairs.size - 1)
        }

        btnSave.setOnClickListener {
            saveComparison()
        }
    }

    private fun saveComparison() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        // Auto-fill empty cells
        val finalPairs = pairs.map { pair ->
            var left = pair.left
            var right = pair.right
            
            if (left.verseReference.isEmpty() && left.text.isEmpty()) {
                left = CustomComparisonItem("", "<italic>[This version uses different verse boundaries than other versions.]</italic>")
            }
            if (right.verseReference.isEmpty() && right.text.isEmpty()) {
                right = CustomComparisonItem("", "<italic>[This version uses different verse boundaries than other versions.]</italic>")
            }
            CustomComparisonPair(left, right)
        }.toMutableList()

        val comparison = CustomComparison(
            id = UUID.randomUUID().toString(),
            title = title,
            pairs = finalPairs
        )

        CustomComparisonRepository.saveComparison(this, comparison)
        Toast.makeText(this, "Comparison Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
