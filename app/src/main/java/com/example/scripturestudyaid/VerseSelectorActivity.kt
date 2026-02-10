package com.example.scripturestudyaid

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

class VerseSelectorActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var rvVerses: RecyclerView
    private lateinit var tvCurrentLocation: TextView


    private var currentChapterIndex = 0
    private var currentBook: Book? = null
    private var currentVolume: String = ""
    private var scriptureData: ScriptureResponse? = null
    
    private var selectedVerse: Verse? = null
    private var adapter: VerseAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verse_selector)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Select a Verse"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        val rootLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        rvVerses = findViewById(R.id.rvVerses)
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)
        // btnConfirmSelection = findViewById(R.id.btnConfirmSelection) // Already found above

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        rvVerses.layoutManager = LinearLayoutManager(this)

        // Check for invalid context from intent (sometimes book names coming from text might not match JSON)
        val initialVolume = intent.getStringExtra("initial_volume") ?: "Book of Mormon"
        val initialBook = intent.getStringExtra("initial_book")
        val initialChapter = intent.getIntExtra("initial_chapter", -1)
        val initialVerse = intent.getIntExtra("initial_verse", -1)
        val preselectedRefs = intent.getStringArrayListExtra("preselected_refs") ?: arrayListOf()

        loadScriptures(initialVolume)

        if (initialBook != null && initialChapter != -1) {
            // Find and set book
            val book = scriptureData?.books?.find { it.book == initialBook }
            if (book != null) {
                currentBook = book
                // Validate chapter index (chapterNum - 1)
                if (initialChapter > 0 && initialChapter <= book.chapters.size) {
                    currentChapterIndex = initialChapter - 1
                }
                
                updateDisplay()
                
                // Scroll to verse if provided
                if (initialVerse != -1) {
                    rvVerses.post {
                        (rvVerses.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(initialVerse - 1, 0)
                    }
                }
            }
        }
        
        // btnConfirmSelection.setOnClickListener { ... } // Removed for immediate selection
        // btnConfirmSelection.isEnabled = false

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })
    }

    private fun loadScriptures(volume: String) {
        currentVolume = volume
        val fileName = when (volume) {
            "Book of Mormon" -> "book-of-mormon.json"
            "Old Testament" -> "old-testament.json"
            "New Testament" -> "new-testament.json"
            "Doctrine and Covenants" -> "doctrine-and-covenants.json"
            "Pearl of Great Price" -> "pearl-of-great-price.json"
            else -> "book-of-mormon.json"
        }

        scriptureData = JsonUtils.getScriptures(this, fileName)
        if (scriptureData != null && scriptureData!!.books.isNotEmpty()) {
            currentBook = scriptureData!!.books[0]
            currentChapterIndex = 0
            updateDisplay()
            updateMenu(volume)
        }
    }

    private fun updateMenu(volume: String) {
        navView.menu.clear()
        
        // Add Volumes Group
        val volumeGroup = navView.menu.addSubMenu("Volumes")
        val volumes = listOf("Old Testament", "New Testament", "Book of Mormon", "Doctrine and Covenants", "Pearl of Great Price")
        for (v in volumes) {
            val item = volumeGroup.add(v)
            item.isCheckable = true
            item.isChecked = v == volume
            item.setOnMenuItemClickListener {
                loadScriptures(v)
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
        }

        // Add Books Group
        if (scriptureData != null) {
            val bookGroup = navView.menu.addSubMenu("Books in $volume")
            for (book in scriptureData!!.books) {
                val item = bookGroup.add(book.book)
                item.isCheckable = true
                item.isChecked = book.book == currentBook?.book
                item.setOnMenuItemClickListener {
                    currentBook = book
                    currentChapterIndex = 0
                    updateDisplay()
                    updateMenu(volume) // Refresh menu to show chapters for this book
                    // drawerLayout.closeDrawer(GravityCompat.START) // Keep open to select chapter? Or close and let them reopen?
                    // User asked for "add that to the side bar", implying they want to select it there.
                    true
                }
            }
            
            // Add Chapters Group for Current Book
            currentBook?.let { book ->
                val chapterGroup = navView.menu.addSubMenu("Chapters in ${book.book}")
                for ((index, chapter) in book.chapters.withIndex()) {
                    val item = chapterGroup.add("Chapter ${chapter.chapter}")
                    item.isCheckable = true
                    item.isChecked = index == currentChapterIndex
                    item.setOnMenuItemClickListener {
                        currentChapterIndex = index
                        updateDisplay()
                        updateMenu(volume)
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                }
            }
        }
    }

    private fun updateDisplay() {
        val book = currentBook ?: return
        val chapter = book.chapters.getOrNull(currentChapterIndex) ?: return

        tvCurrentLocation.text = "${book.book} ${chapter.chapter}"
        
        // Reuse VerseAdapter but in selection mode
        adapter = VerseAdapter(
            chapter.verses,
            volumeName = currentVolume,
            bookName = book.book,
            chapterNum = chapter.chapter,
            preselectedVerses = intent.getStringArrayListExtra("preselected_refs") ?: emptyList(),
            isComparisonMode = true
        )
        
        adapter?.setSelectionMode(true) { verse ->
            if (verse.verse != -1) {
                val resultIntent = Intent()
                resultIntent.putExtra("verse_text", verse.text)
                resultIntent.putExtra("verse_ref", "${currentBook?.book} ${currentChapterIndex + 1}:${verse.verse}")
                
                // Return current context
                resultIntent.putExtra("return_volume", currentVolume)
                resultIntent.putExtra("return_book", currentBook?.book)
                resultIntent.putExtra("return_chapter", currentChapterIndex + 1)
                resultIntent.putExtra("return_verse", verse.verse)
                
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
        
        rvVerses.adapter = adapter
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle explicit menu items if we had them in XML, 
        // but we generate them dynamically in updateMenu
        return true
    }
}
