package com.example.scripturestudyaid

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : BaseActivity() {

    private lateinit var bibleData: ScriptureResponse
    private lateinit var verseAdapter: VerseAdapter

    private var currentBookIndex = 0
    private var currentChapterIndex = 0

    private val volumes = mapOf(
        "Book of Mormon" to "book-of-mormon.json",
        "Old Testament" to "old-testament.json",
        "New Testament" to "new-testament.json",
        "Doctrine and Covenants" to "doctrine-and-covenants.json",
        "Pearl of Great Price" to "pearl-of-great-price.json"
    )

    private var currentVolumeName = "Book of Mormon"
    private var isSearchVisible = false
    private var searchScope = "chapter" // "chapter", "book", "selected", "all"
    private var selectedBooksForSearch = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar elements
        val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)
        val tvToolbarSubtitle = findViewById<TextView>(R.id.tvToolbarSubtitle)
        val layoutTitleSection = findViewById<View>(R.id.layoutTitleSection)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val btnBookmark = findViewById<ImageButton>(R.id.btnBookmark)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        
        // Search bar
        val searchBarContainer = findViewById<View>(R.id.searchBarContainer)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnSearchScope = findViewById<Button>(R.id.btnSearchScope)
        val tvSearchScope = findViewById<TextView>(R.id.tvSearchScope)

        loadScriptures(volumes[currentVolumeName]!!)
        updateToolbarText(tvToolbarTitle, tvToolbarSubtitle)

        // Back button - navigate to home screen
        btnBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Title section click - show navigation dialog
        layoutTitleSection.setOnClickListener {
            showNavigationDialog(tvToolbarTitle, tvToolbarSubtitle)
        }

        // Search button
        btnSearch.setOnClickListener {
            isSearchVisible = !isSearchVisible
            searchBarContainer.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
            if (!isSearchVisible) {
                etSearch.text.clear()
                if (searchScope == "chapter") {
                    verseAdapter.filterVerses("") // Reset filter for current chapter
                }
            }
        }

        // Search scope button
        btnSearchScope.setOnClickListener {
            showSearchScopeDialog(btnSearchScope, tvSearchScope)
        }

        // Search text change listener
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString()
                when (searchScope) {
                    "chapter" -> verseAdapter.filterVerses(query)
                    "book", "selected", "all" -> {
                        if (query.length >= 2) { // Only search with 2+ characters
                            performCrossReferenceSearch(query)
                        }
                    }
                }
            }
        })

        // Bookmark button - toggle bookmark for current chapter
        btnBookmark.setOnClickListener {
            val bookmark = Bookmark(
                id = java.util.UUID.randomUUID().toString(),
                volume = currentVolumeName,
                book = bibleData.books[currentBookIndex].book,
                chapter = bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter,
                verse = 1, // Bookmarking the chapter
                timestamp = System.currentTimeMillis()
            )
            AnnotationRepository.saveBookmark(this, bookmark)
            Toast.makeText(this, "Chapter bookmarked", Toast.LENGTH_SHORT).show()
        }

        // Menu button
        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 0, 0, "Settings")
            popup.menu.add(0, 1, 1, "View Bookmarks")
            popup.menu.add(0, 2, 2, "About")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    1 -> showBookmarksDialog()
                    2 -> Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
                }
                true
            }
            popup.show()
        }
    }

    private fun showNavigationDialog(tvTitle: TextView, tvSubtitle: TextView) {
        val dialogView = android.widget.LinearLayout(this)
        dialogView.orientation = android.widget.LinearLayout.VERTICAL
        dialogView.setPadding(40, 20, 40, 20)

        // Volume selector
        val volumeLabel = TextView(this)
        volumeLabel.text = "Volume"
        volumeLabel.setTextColor(android.graphics.Color.BLACK)
        volumeLabel.textSize = 14f
        dialogView.addView(volumeLabel)

        val volumeButton = Button(this)
        volumeButton.text = currentVolumeName
        
        // Book selector (declared here so volume callback can update it)
        val bookButton = Button(this)
        val chapterButton = Button(this)
        
        volumeButton.setOnClickListener {
            showVolumeSelector { selectedVolume ->
                currentVolumeName = selectedVolume
                volumeButton.text = selectedVolume
                currentBookIndex = 0
                currentChapterIndex = 0
                loadScriptures(volumes[currentVolumeName]!!)
                // Update the book and chapter buttons to reflect the new volume
                bookButton.text = bibleData.books[currentBookIndex].book
                chapterButton.text = "Chapter ${bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter}"
            }
        }
        dialogView.addView(volumeButton)

        // Book selector
        val bookLabel = TextView(this)
        bookLabel.text = "Book"
        bookLabel.setTextColor(android.graphics.Color.BLACK)
        bookLabel.textSize = 14f
        dialogView.addView(bookLabel)

        bookButton.text = bibleData.books[currentBookIndex].book
        bookButton.setOnClickListener {
            showBookSelector { selectedBookIndex ->
                currentBookIndex = selectedBookIndex
                currentChapterIndex = 0
                bookButton.text = bibleData.books[currentBookIndex].book
                // Update chapter button when book changes
                chapterButton.text = "Chapter ${bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter}"
            }
        }
        dialogView.addView(bookButton)

        // Chapter selector
        val chapterLabel = TextView(this)
        chapterLabel.text = "Chapter"
        chapterLabel.setTextColor(android.graphics.Color.BLACK)
        chapterLabel.textSize = 14f
        dialogView.addView(chapterLabel)

        chapterButton.text = "Chapter ${bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter}"
        chapterButton.setOnClickListener {
            showChapterSelector { selectedChapterIndex ->
                currentChapterIndex = selectedChapterIndex
                chapterButton.text = "Chapter ${bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter}"
            }
        }
        dialogView.addView(chapterButton)

        AlertDialog.Builder(this)
            .setTitle("Navigate")
            .setView(dialogView)
            .setPositiveButton("Go") { _, _ ->
                updateToolbarText(tvTitle, tvSubtitle)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVolumeSelector(onSelected: (String) -> Unit) {
        val volumeNames = volumes.keys.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Volume")
            .setItems(volumeNames) { _, which ->
                onSelected(volumeNames[which])
            }
            .show()
    }

    private fun showBookSelector(onSelected: (Int) -> Unit) {
        val bookNames = bibleData.books.map { it.book }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Book")
            .setItems(bookNames) { _, which ->
                onSelected(which)
            }
            .show()
    }

    private fun showChapterSelector(onSelected: (Int) -> Unit) {
        val chapterNames = bibleData.books[currentBookIndex].chapters.mapIndexed { index, chapter ->
            "Chapter ${chapter.chapter}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Chapter")
            .setItems(chapterNames) { _, which ->
                onSelected(which)
            }
            .show()
    }

    private fun loadScriptures(fileName: String) {
        val loadedData = JsonUtils.getScriptures(this, fileName)
        if (loadedData != null) {
            bibleData = loadedData
            if (::verseAdapter.isInitialized) {
                 verseAdapter.updateVerses(
                    bibleData.books[currentBookIndex].chapters[currentChapterIndex].verses,
                    currentVolumeName,
                    bibleData.books[currentBookIndex].book,
                    bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter
                 )
            } else {
                 val rvVerses = findViewById<RecyclerView>(R.id.rvVerses)
                 verseAdapter = VerseAdapter(
                    bibleData.books[currentBookIndex].chapters[currentChapterIndex].verses,
                    currentVolumeName,
                    bibleData.books[currentBookIndex].book,
                    bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter
                 )
                 rvVerses.adapter = verseAdapter
                 rvVerses.layoutManager = LinearLayoutManager(this)
                 
                 // Add swipe gesture detection for chapter navigation
                 setupSwipeGesture(rvVerses)
            }
        }
    }

    private fun updateToolbarText(tvTitle: TextView, tvSubtitle: TextView) {
        tvTitle.text = currentVolumeName
        val selectedChapter = bibleData.books[currentBookIndex].chapters[currentChapterIndex]
        tvSubtitle.text = "${bibleData.books[currentBookIndex].book} ${selectedChapter.chapter}"

        // Tell the list to show the new verses
        verseAdapter.updateVerses(
            selectedChapter.verses,
            currentVolumeName,
            bibleData.books[currentBookIndex].book,
            selectedChapter.chapter
        )
    }

    private fun showSearchScopeDialog(btnScope: Button, tvScope: TextView) {
        val options = arrayOf("Current Chapter", "Current Book", "Select Books", "All Scriptures")
        AlertDialog.Builder(this)
            .setTitle("Search Scope")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        searchScope = "chapter"
                        btnScope.text = "Chapter"
                        tvScope.text = "Searching in: Current Chapter"
                    }
                    1 -> {
                        searchScope = "book"
                        btnScope.text = "Book"
                        tvScope.text = "Searching in: ${bibleData.books[currentBookIndex].book}"
                    }
                    2 -> {
                        showMultipleBookSelector(btnScope, tvScope)
                    }
                    3 -> {
                        searchScope = "all"
                        btnScope.text = "All"
                        tvScope.text = "Searching in: All Scriptures"
                    }
                }
            }
            .show()
    }

    private fun showMultipleBookSelector(btnScope: Button, tvScope: TextView) {
        val allBooks = bibleData.books.map { it.book }.toTypedArray()
        val checkedItems = BooleanArray(allBooks.size) { false }
        
        AlertDialog.Builder(this)
            .setTitle("Select Books to Search")
            .setMultiChoiceItems(allBooks, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                selectedBooksForSearch.clear()
                checkedItems.forEachIndexed { index, isChecked ->
                    if (isChecked) {
                        selectedBooksForSearch.add(allBooks[index])
                    }
                }
                if (selectedBooksForSearch.isNotEmpty()) {
                    searchScope = "selected"
                    btnScope.text = "Custom"
                    tvScope.text = "Searching in: ${selectedBooksForSearch.size} books"
                } else {
                    Toast.makeText(this, "No books selected", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCrossReferenceSearch(query: String) {
        val results = mutableListOf<SearchResult>()
        val lowerQuery = query.lowercase()

        when (searchScope) {
            "book" -> {
                val currentBook = bibleData.books[currentBookIndex]
                currentBook.chapters.forEach { chapter ->
                    chapter.verses.forEach { verse ->
                        if (verse.text.lowercase().contains(lowerQuery)) {
                            results.add(SearchResult(currentVolumeName, currentBook.book, chapter.chapter, verse.verse, verse.text))
                        }
                    }
                }
            }
            "selected" -> {
                selectedBooksForSearch.forEach { bookName ->
                    val book = bibleData.books.find { it.book == bookName }
                    book?.chapters?.forEach { chapter ->
                        chapter.verses.forEach { verse ->
                            if (verse.text.lowercase().contains(lowerQuery)) {
                                results.add(SearchResult(currentVolumeName, book.book, chapter.chapter, verse.verse, verse.text))
                            }
                        }
                    }
                }
            }
            "all" -> {
                volumes.forEach { (volName, fileName) ->
                    val volumeData = JsonUtils.getScriptures(this, fileName)
                    volumeData?.books?.forEach { book ->
                        book.chapters.forEach { chapter ->
                            chapter.verses.forEach { verse ->
                                if (verse.text.lowercase().contains(lowerQuery)) {
                                    results.add(SearchResult(volName, book.book, chapter.chapter, verse.verse, verse.text))
                                }
                            }
                        }
                    }
                }
            }
        }

        showSearchResults(results, query)
    }

    private fun showSearchResults(results: List<SearchResult>, query: String) {
        val resultStrings = results.take(100).map { result ->
            val highlightedText = result.verseText.replace(query, "[$query]", ignoreCase = true)
            "${result.volume} - ${result.book} ${result.chapter}:${result.verse}\n\"$highlightedText\""
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Search Results (${results.size} found)")
            .setItems(resultStrings) { _, which ->
                val result = results[which]
                navigateToVerse(result)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun navigateToVerse(result: SearchResult) {
        if (result.volume != currentVolumeName) {
            currentVolumeName = result.volume
            loadScriptures(volumes[currentVolumeName]!!)
        }

        currentBookIndex = bibleData.books.indexOfFirst { it.book == result.book }
        if (currentBookIndex == -1) return

        currentChapterIndex = bibleData.books[currentBookIndex].chapters.indexOfFirst { it.chapter == result.chapter }
        if (currentChapterIndex == -1) return

        val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)
        val tvToolbarSubtitle = findViewById<TextView>(R.id.tvToolbarSubtitle)
        updateToolbarText(tvToolbarTitle, tvToolbarSubtitle)

        Toast.makeText(this, "Navigated to ${result.book} ${result.chapter}:${result.verse}", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarksDialog() {
        val bookmarks = AnnotationRepository.getAllBookmarks(this)
        
        if (bookmarks.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Bookmarks")
                .setMessage("No bookmarks saved yet.\n\nTap the bookmark icon to save your current chapter.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        val bookmarkStrings = bookmarks.map { bookmark ->
            "${bookmark.volume} - ${bookmark.book} ${bookmark.chapter}"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Bookmarks (${bookmarks.size})")
            .setItems(bookmarkStrings) { _, which ->
                val bookmark = bookmarks[which]
                showBookmarkOptionsDialog(bookmark)
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showBookmarkOptionsDialog(bookmark: Bookmark) {
        val message = "${bookmark.volume}\n${bookmark.book} ${bookmark.chapter}"
        
        AlertDialog.Builder(this)
            .setTitle("Bookmark")
            .setMessage(message)
            .setPositiveButton("Go to Chapter") { _, _ ->
                navigateToBookmark(bookmark)
            }
            .setNegativeButton("Delete") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Bookmark?")
                    .setMessage("Remove this bookmark?")
                    .setPositiveButton("Delete") { _, _ ->
                        AnnotationRepository.deleteBookmark(this, bookmark.id)
                        Toast.makeText(this, "Bookmark deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
    
    private fun navigateToBookmark(bookmark: Bookmark) {
        // Load the volume if different
        if (bookmark.volume != currentVolumeName) {
            currentVolumeName = bookmark.volume
            loadScriptures(volumes[currentVolumeName]!!)
        }
        
        // Find the book index
        currentBookIndex = bibleData.books.indexOfFirst { it.book == bookmark.book }
        if (currentBookIndex == -1) {
            Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Find the chapter index
        currentChapterIndex = bibleData.books[currentBookIndex].chapters.indexOfFirst {
            it.chapter == bookmark.chapter
        }
        if (currentChapterIndex == -1) {
            Toast.makeText(this, "Chapter not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update the toolbar and verses
        val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)
        val tvToolbarSubtitle = findViewById<TextView>(R.id.tvToolbarSubtitle)
        updateToolbarText(tvToolbarTitle, tvToolbarSubtitle)
        
        Toast.makeText(this, "Navigated to ${bookmark.book} ${bookmark.chapter}", Toast.LENGTH_SHORT).show()
    }

    private fun setupSwipeGesture(recyclerView: RecyclerView) {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - go to previous chapter
                            goToPreviousChapter()
                        } else {
                            // Swipe left - go to next chapter
                            goToNextChapter()
                        }
                        return true
                    }
                }
                return false
            }
        })
        
        recyclerView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            false // Allow RecyclerView to handle scrolling
        }
    }
    
    private fun goToNextChapter() {
        // Try to go to next chapter in current book
        if (currentChapterIndex < bibleData.books[currentBookIndex].chapters.size - 1) {
            currentChapterIndex++
        } else if (currentBookIndex < bibleData.books.size - 1) {
            // Go to first chapter of next book
            currentBookIndex++
            currentChapterIndex = 0
        } else {
            Toast.makeText(this, "Last chapter in ${currentVolumeName}", Toast.LENGTH_SHORT).show()
            return
        }
        
        val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)
        val tvToolbarSubtitle = findViewById<TextView>(R.id.tvToolbarSubtitle)
        updateToolbarText(tvToolbarTitle, tvToolbarSubtitle)
        
        // Scroll to top
        findViewById<RecyclerView>(R.id.rvVerses).scrollToPosition(0)
    }
    
    private fun goToPreviousChapter() {
        // Try to go to previous chapter in current book
        if (currentChapterIndex > 0) {
            currentChapterIndex--
        } else if (currentBookIndex > 0) {
            // Go to last chapter of previous book
            currentBookIndex--
            currentChapterIndex = bibleData.books[currentBookIndex].chapters.size - 1
        } else {
            Toast.makeText(this, "First chapter in ${currentVolumeName}", Toast.LENGTH_SHORT).show()
            return
        }
        
        val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)
        val tvToolbarSubtitle = findViewById<TextView>(R.id.tvToolbarSubtitle)
        updateToolbarText(tvToolbarTitle, tvToolbarSubtitle)
        
        // Scroll to top
        findViewById<RecyclerView>(R.id.rvVerses).scrollToPosition(0)
    }

    data class SearchResult(val volume: String, val book: String, val chapter: Int, val verse: Int, val verseText: String)
}
