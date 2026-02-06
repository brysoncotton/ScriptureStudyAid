package com.example.scripturestudyaid

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : BaseActivity(), HighlightBottomSheetFragment.OnHighlightOptionSelectedListener {

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

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        val btnSelectVolume = findViewById<Button>(R.id.btnSelectVolume)
        val btnSelectBook = findViewById<Button>(R.id.btnSelectBook)
        val btnSelectChapter = findViewById<Button>(R.id.btnSelectChapter)


        loadScriptures(volumes[currentVolumeName]!!)
        updateButtons(btnSelectBook, btnSelectChapter)
        btnSelectVolume.text = currentVolumeName

        // Handle Volume Selection
        btnSelectVolume.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            volumes.keys.forEachIndexed { index, volumeName ->
                popup.menu.add(0, index, 0, volumeName)
            }
            popup.setOnMenuItemClickListener { item ->
                val selectedName = item.title.toString()
                if (currentVolumeName != selectedName) {
                    currentVolumeName = selectedName
                    btnSelectVolume.text = currentVolumeName
                    
                    // Reset indices
                    currentBookIndex = 0
                    currentChapterIndex = 0
                    
                    loadScriptures(volumes[currentVolumeName]!!)
                    updateButtons(btnSelectBook, btnSelectChapter)
                }
                true
            }
            popup.show()
        }

        // Handle Book Selection
        btnSelectBook.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            bibleData.books.forEachIndexed { index, book ->
                popup.menu.add(0, index, 0, book.book)
            }
            popup.setOnMenuItemClickListener { item ->
                currentBookIndex = item.itemId
                currentChapterIndex = 0 // Reset to chapter 1 when book changes
                
                updateButtons(btnSelectBook, btnSelectChapter)
                true
            }
            popup.show()
        }

        // Handle Chapter Selection
        btnSelectChapter.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            val chapters = bibleData.books[currentBookIndex].chapters
            chapters.forEachIndexed { index, chapter ->
                popup.menu.add(0, index, 0, "Chapter ${chapter.chapter}")
            }
            popup.setOnMenuItemClickListener { item ->
                currentChapterIndex = item.itemId
                updateButtons(btnSelectBook, btnSelectChapter)
                true
            }
            popup.show()
        }
    }

    private fun loadScriptures(fileName: String) {
        val loadedData = JsonUtils.getScriptures(this, fileName)
        if (loadedData != null) {
            bibleData = loadedData
            // If adapter exists, update it, otherwise create it
            if (::verseAdapter.isInitialized) {
                 updateContent()
            } else {
                 val rvVerses = findViewById<RecyclerView>(R.id.rvVerses)
                 val currentBook = bibleData.books[currentBookIndex]
                 val currentChapter = currentBook.chapters[currentChapterIndex]
                 val highlights = database.highlightDao().getHighlightsForChapter(
                     currentVolumeName,
                     currentBook.book,
                     currentChapter.chapter
                 )
                 
                 verseAdapter = VerseAdapter(currentChapter.verses)
                 verseAdapter.setOnVerseInteractionListener(::onVerseInteraction)
                 verseAdapter.updateData(currentChapter.verses, highlights)
                 rvVerses.adapter = verseAdapter
                 rvVerses.layoutManager = LinearLayoutManager(this)
            }
        }
    }

    private fun updateButtons(bookButton: Button, chapterButton: Button) {
        bookButton.text = bibleData.books[currentBookIndex].book
        val selectedChapter = bibleData.books[currentBookIndex].chapters[currentChapterIndex]
        chapterButton.text = "Chapter ${selectedChapter.chapter}"

        // Tell the list to show the new verses
        updateContent()
    }

    private fun updateContent() {
        val currentBook = bibleData.books[currentBookIndex]
        val currentChapter = currentBook.chapters[currentChapterIndex]
        val highlights = database.highlightDao().getHighlightsForChapter(
            currentVolumeName,
            currentBook.book,
            currentChapter.chapter
        )
        verseAdapter.updateData(currentChapter.verses, highlights)
    }

    private var pendingHighlightVerse: Verse? = null
    private var isEditMode: Boolean = false
    private var editingHighlight: Highlight? = null

    // Called when user touches a verse
    private fun onVerseInteraction(verse: Verse, existingHighlight: Highlight?) {
        this.pendingHighlightVerse = verse
        this.editingHighlight = existingHighlight
        this.isEditMode = existingHighlight != null

        val bottomSheet = HighlightBottomSheetFragment()
        bottomSheet.setListener(this)
        bottomSheet.setEditMode(isEditMode)

        val textLength = verse.text.length
        
        if (isEditMode) {
            // For editing, set range to existing highlight (adjusting for verse number prefix if stored relative to full string, 
            // but our Model stores relative to text content, so simple mapping)
            bottomSheet.setInitialRange(existingHighlight!!.startOne, existingHighlight.endOne, textLength)
        } else {
            // For new, default to full verse or some initial range
            bottomSheet.setInitialRange(0, textLength, textLength)
            // Show initial preview
            updatePreview(verse, 0, textLength, Color.YELLOW, "SOLID")
        }
        
        bottomSheet.show(supportFragmentManager, HighlightBottomSheetFragment.TAG)
    }
    
    // Updates the adapter's preview highlight
    private fun updatePreview(verse: Verse, start: Int, end: Int, color: Int = Color.YELLOW, type: String = "SOLID") {
        val preview = Highlight(
             volume = currentVolumeName,
             book = bibleData.books[currentBookIndex].book,
             chapter = bibleData.books[currentBookIndex].chapters[currentChapterIndex].chapter,
             verse = verse.verse,
             startOne = start,
             endOne = end,
             color = color,
             type = type
        )
        verseAdapter.setPreviewHighlight(preview)
    }

    override fun onSliderValueChanged(start: Int, end: Int) {
        val verse = pendingHighlightVerse ?: return
        // Use current color/type if available, or defaults. 
        // ideally we track current state in fragment and pass back, or track here.
        // For simplicity, defaulting to Edit highlight's props or defaults
        val color = editingHighlight?.color ?: Color.YELLOW
        val type = editingHighlight?.type ?: "SOLID"
        updatePreview(verse, start, end, color, type)
    }

    override fun onHighlightOptionSelected(color: Int, type: String, start: Int, end: Int) {
        val verse = pendingHighlightVerse ?: return
        val currentBook = bibleData.books[currentBookIndex]
        val currentChapter = currentBook.chapters[currentChapterIndex]
        
        // If editing, delete original first (replace operation)
        if (editingHighlight != null) {
            database.highlightDao().delete(editingHighlight!!)
        }
        
        val highlight = Highlight(
            volume = currentVolumeName,
            book = currentBook.book,
            chapter = currentChapter.chapter,
            verse = verse.verse,
            startOne = start,
            endOne = end,
            color = color,
            type = type
        )
        database.highlightDao().insert(highlight)
        
        // Clear preview and reload
        verseAdapter.setPreviewHighlight(null)
        updateContent()
        
        // Reset pending
        pendingHighlightVerse = null
        editingHighlight = null
    }
    
    override fun onHighlightDeleted() {
        if (editingHighlight != null) {
            database.highlightDao().delete(editingHighlight!!)
            verseAdapter.setPreviewHighlight(null)
            updateContent()
        }
        pendingHighlightVerse = null
        editingHighlight = null
    }
}