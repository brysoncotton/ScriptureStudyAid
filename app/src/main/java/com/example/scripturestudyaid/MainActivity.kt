package com.example.scripturestudyaid

import android.os.Bundle
import android.widget.Button
import android.widget.PopupMenu
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
                 
                 verseAdapter = VerseAdapter(currentChapter.verses, ::onHighlightSelected)
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

    private fun onHighlightSelected(verse: Verse, start: Int, end: Int) {
        val currentBook = bibleData.books[currentBookIndex]
        val currentChapter = currentBook.chapters[currentChapterIndex]
        val highlight = Highlight(
            volume = currentVolumeName,
            book = currentBook.book,
            chapter = currentChapter.chapter,
            verse = verse.verse,
            startOne = start,
            endOne = end,
            color = android.graphics.Color.YELLOW // Default highlight color
        )
        database.highlightDao().insert(highlight)
        updateContent()
    }
}