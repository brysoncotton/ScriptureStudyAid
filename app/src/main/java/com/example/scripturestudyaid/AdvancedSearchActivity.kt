package com.example.scripturestudyaid

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdvancedSearchActivity : BaseActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var layoutFrequency: LinearLayout
    private lateinit var layoutProximity: LinearLayout
    private lateinit var toggleChartMode: android.widget.ToggleButton

    // Frequency UI
    private lateinit var etFrequencyQuery: EditText
    private lateinit var btnFrequencySearch: Button
    private lateinit var frequencyChart: FrequencyChartView

    // Proximity UI
    private lateinit var etTerm1: EditText
    private lateinit var etTerm2: EditText
    private lateinit var tvDistanceValue: TextView
    private lateinit var seekBarDistance: SeekBar
    private lateinit var btnProximitySearch: Button
    private lateinit var rvProximityResults: RecyclerView

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_search)

        // Init Views
        tabLayout = findViewById(R.id.tabLayout)
        layoutFrequency = findViewById(R.id.layoutFrequency)
        layoutProximity = findViewById(R.id.layoutProximity)

        etFrequencyQuery = findViewById(R.id.etFrequencyQuery)
        btnFrequencySearch = findViewById(R.id.btnFrequencySearch)
        frequencyChart = findViewById(R.id.frequencyChart)
        toggleChartMode = findViewById(R.id.toggleChartMode)

        etTerm1 = findViewById(R.id.etTerm1)
        etTerm2 = findViewById(R.id.etTerm2)
        tvDistanceValue = findViewById(R.id.tvDistanceValue)
        seekBarDistance = findViewById(R.id.seekBarDistance)
        btnProximitySearch = findViewById(R.id.btnProximitySearch)
        rvProximityResults = findViewById(R.id.rvProximityResults)

        rvProximityResults.layoutManager = LinearLayoutManager(this)

        setupTabs()
        setupFrequencySearch()
        setupProximitySearch()
        
        toggleChartMode.setOnCheckedChangeListener { _, _ ->
            if (etFrequencyQuery.text.isNotEmpty()) {
                btnFrequencySearch.performClick()
            }
        }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutFrequency.visibility = View.VISIBLE
                        layoutProximity.visibility = View.GONE
                    }
                    1 -> {
                        layoutFrequency.visibility = View.GONE
                        layoutProximity.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }



    private fun setupFrequencySearch() {
        btnFrequencySearch.setOnClickListener {
            val queryInput = etFrequencyQuery.text.toString().trim()
            if (queryInput.isEmpty()) {
                etFrequencyQuery.error = "Enter word(s)"
                return@setOnClickListener
            }

            val queries = queryInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val isGrouped = toggleChartMode.isChecked

            scope.launch {
                btnFrequencySearch.isEnabled = false
                btnFrequencySearch.text = "Analyzing..."
                
                try {
                    val data = SearchEngine.getWordFrequencies(this@AdvancedSearchActivity, queries)
                    if (data.isEmpty()) {
                        Toast.makeText(this@AdvancedSearchActivity, "No matches found", Toast.LENGTH_SHORT).show()
                    }
                    // stacked = !isGrouped
                    frequencyChart.setData(data, stacked = !isGrouped)
                    
                    frequencyChart.setOnBarClickListener(object : FrequencyChartView.OnBarClickListener {
                        override fun onBarClick(label: String) {
                            // Drill down only supports single query for now?? 
                            // Or we pick the first one? Or show dialog with all?
                            // Let's just use the full input string for the drill down title
                            showChapterFrequencyDialog(label, queryInput)
                        }
                    })
                } finally {
                    btnFrequencySearch.isEnabled = true
                    btnFrequencySearch.text = "Analyze"
                }
            }
        }
    }

    private fun showChapterFrequencyDialog(bookName: String, query: String) {
        val dialogView = layoutInflater.inflate(R.layout.activity_advanced_search, null) as LinearLayout
        // Strip out unnecessary children from the inflated view to reuse just the structure or create new
        // Actually, let's just create a simple layout programmatically or reuse a part
        
        // Simpler: Create a layout with just the chart
        val chartContainer = android.widget.HorizontalScrollView(this)
        chartContainer.isFillViewport = true
        
        val chart = FrequencyChartView(this)
        chart.minimumHeight = 500
        chart.minimumWidth = 1000
        chartContainer.addView(chart)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("$bookName - Frequency of '$query'")
            .setView(chartContainer)
            .setPositiveButton("Close", null)
            .create()
            
        dialog.show()
        
        val isGrouped = toggleChartMode.isChecked
        
        scope.launch {
            val queries = query.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val data = SearchEngine.getChapterFrequencies(this@AdvancedSearchActivity, bookName, queries)
            chart.setData(data, stacked = !isGrouped)
            
            chart.setOnBarClickListener(object : FrequencyChartView.OnBarClickListener {
                override fun onBarClick(label: String) {
                    // label is chapter number string e.g. "1"
                    val chapterNum = label.toIntOrNull()
                    if (chapterNum != null) {
                         val intent = android.content.Intent(this@AdvancedSearchActivity, MainActivity::class.java)
                         intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                         intent.putExtra("EXTRA_BOOK", bookName)
                         intent.putExtra("EXTRA_CHAPTER", chapterNum)
                         startActivity(intent)
                         // Optional: Close dialog or finish activity? User might want to go back to search.
                         // Let's keep activity open but navigate main. 
                         // But since we clear top MainActivity, AdvancedSearch might be cleared if it is on top of Main?
                         // MainActivity launchMode singleTop logic:
                         // If Main is below Advanced, CLEAR_TOP will destroy Advanced. 
                         // If user wants to return to search, they might lose state.
                         // Let's just launch Main. If they want to search again, they click "Advanced Search" in Main.
                    }
                }
            })
        }
    }

    private fun setupProximitySearch() {
        seekBarDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDistanceValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnProximitySearch.setOnClickListener {
            val term1 = etTerm1.text.toString().trim()
            val term2 = etTerm2.text.toString().trim()
            val distance = seekBarDistance.progress

            if (term1.isEmpty() || term2.isEmpty()) {
                Toast.makeText(this, "Enter both terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                btnProximitySearch.isEnabled = false
                btnProximitySearch.text = "Searching..."
                
                try {
                    val results = SearchEngine.proximitySearch(this@AdvancedSearchActivity, term1, term2, distance)
                    
                    if (results.isEmpty()) {
                        Toast.makeText(this@AdvancedSearchActivity, "No matches found within $distance words", Toast.LENGTH_SHORT).show()
                        rvProximityResults.adapter = null
                    } else {
                         // We need a simple adapter for results. 
                         // Since we don't have one readily available for this format, let's create a quick anonymous one or reuse VerseAdapter if possible?
                         // VerseAdapter expects a list of Strings (verses). 
                         // Let's repurpose a simple list for now using a basic adapter.
                         showResults(results, term1, term2)
                    }
                } finally {
                    btnProximitySearch.isEnabled = true
                    btnProximitySearch.text = "Find Similarities"
                }
            }
        }
    }

    private fun showResults(results: List<SearchResult>, term1: String, term2: String) {
         // Since we don't have a dedicated adapter for this, let's use a dialog or text for now, 
         // OR better, create a simple adapter in this file or a separate one.
         // For speed, let's use the existing VerseAdapter but we need to map SearchResult to string? No VerseAdapter takes Verse objects.
         // Let's create a simple inner adapter.
         
         val adapter = ProximityAdapter(results) { result ->
             // Handle navigation
             // We can't easily navigate back to MainActivity's specific state from here without passing complex data 
             // OR finishing this activity and returning result.
             // For now, let's just show a toast or dialog with full text.
             AlertDialog.Builder(this)
                 .setTitle("${result.book} ${result.chapter}:${result.verse}")
                 .setMessage(result.verseText)
                 .setPositiveButton("OK", null)
                 .show()
         }
         rvProximityResults.adapter = adapter
    }
}

class ProximityAdapter(
    private val results: List<SearchResult>,
    private val onClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<ProximityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRef: TextView = view.findViewById(android.R.id.text1)
        val tvText: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.tvRef.text = "${result.volume} - ${result.book} ${result.chapter}:${result.verse}"
        holder.tvText.text = result.verseText.take(100) + "..." // Preview
        holder.tvText.maxLines = 2
        
        holder.itemView.setOnClickListener { onClick(result) }
    }

    override fun getItemCount() = results.size
}
