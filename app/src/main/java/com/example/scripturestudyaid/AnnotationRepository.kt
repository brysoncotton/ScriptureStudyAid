package com.example.scripturestudyaid

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object AnnotationRepository {
    private const val TAGS_FILENAME = "tags.json"
    private const val BOOKMARKS_FILENAME = "bookmarks.json"
    private const val LINKS_FILENAME = "links.json"
    private const val HIGHLIGHTS_FILENAME = "highlights.json"
    private val gson = Gson()

    // ================== TAGS ==================
    
    private fun getTagsFile(context: Context): File {
        return File(context.filesDir, TAGS_FILENAME)
    }

    fun getAllTags(context: Context): List<Tag> {
        val file = getTagsFile(context)
        if (!file.exists()) return emptyList()
        
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<Tag>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveTag(context: Context, tag: Tag) {
        val tags = getAllTags(context).toMutableList()
        // Remove existing tag with same id if present
        tags.removeAll { it.id == tag.id }
        tags.add(tag)
        saveTagsInternal(context, tags)
    }

    fun deleteTag(context: Context, tagId: String) {
        val tags = getAllTags(context).toMutableList()
        tags.removeAll { it.id == tagId }
        saveTagsInternal(context, tags)
    }

    private fun saveTagsInternal(context: Context, tags: List<Tag>) {
        try {
            val jsonString = gson.toJson(tags)
            getTagsFile(context).writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTagsForVerse(context: Context, volume: String, book: String, chapter: Int, verse: Int): List<Tag> {
        return getAllTags(context).filter {
            it.volume == volume && it.book == book && it.chapter == chapter && it.verse == verse
        }
    }

    fun searchTagsByName(context: Context, tagName: String): List<Tag> {
        return getAllTags(context).filter {
            it.tagName.contains(tagName, ignoreCase = true)
        }
    }

    // ================== BOOKMARKS ==================
    
    private fun getBookmarksFile(context: Context): File {
        return File(context.filesDir, BOOKMARKS_FILENAME)
    }

    fun getAllBookmarks(context: Context): List<Bookmark> {
        val file = getBookmarksFile(context)
        if (!file.exists()) return emptyList()
        
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<Bookmark>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveBookmark(context: Context, bookmark: Bookmark) {
        val bookmarks = getAllBookmarks(context).toMutableList()
        bookmarks.removeAll { it.id == bookmark.id }
        bookmarks.add(bookmark)
        saveBookmarksInternal(context, bookmarks)
    }

    fun deleteBookmark(context: Context, bookmarkId: String) {
        val bookmarks = getAllBookmarks(context).toMutableList()
        bookmarks.removeAll { it.id == bookmarkId }
        saveBookmarksInternal(context, bookmarks)
    }

    private fun saveBookmarksInternal(context: Context, bookmarks: List<Bookmark>) {
        try {
            val jsonString = gson.toJson(bookmarks)
            getBookmarksFile(context).writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBookmarkForVerse(context: Context, volume: String, book: String, chapter: Int, verse: Int): Bookmark? {
        return getAllBookmarks(context).firstOrNull {
            it.volume == volume && it.book == book && it.chapter == chapter && it.verse == verse
        }
    }

    fun toggleBookmark(context: Context, volume: String, book: String, chapter: Int, verse: Int): Boolean {
        val existing = getBookmarkForVerse(context, volume, book, chapter, verse)
        return if (existing != null) {
            deleteBookmark(context, existing.id)
            false // Bookmark removed
        } else {
            val bookmark = Bookmark(
                id = java.util.UUID.randomUUID().toString(),
                volume = volume,
                book = book,
                chapter = chapter,
                verse = verse,
                timestamp = System.currentTimeMillis()
            )
            saveBookmark(context, bookmark)
            true // Bookmark added
        }
    }

    // ================== LINKS ==================
    
    private fun getLinksFile(context: Context): File {
        return File(context.filesDir, LINKS_FILENAME)
    }

    fun getAllLinks(context: Context): List<Link> {
        val file = getLinksFile(context)
        if (!file.exists()) return emptyList()
        
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<Link>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveLink(context: Context, link: Link) {
        val links = getAllLinks(context).toMutableList()
        links.removeAll { it.id == link.id }
        links.add(link)
        saveLinksInternal(context, links)
    }

    fun deleteLink(context: Context, linkId: String) {
        val links = getAllLinks(context).toMutableList()
        links.removeAll { it.id == linkId }
        saveLinksInternal(context, links)
    }

    private fun saveLinksInternal(context: Context, links: List<Link>) {
        try {
            val jsonString = gson.toJson(links)
            getLinksFile(context).writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLinksForVerse(context: Context, volume: String, book: String, chapter: Int, verse: Int): List<Link> {
        return getAllLinks(context).filter {
            (it.sourceVolume == volume && it.sourceBook == book && it.sourceChapter == chapter && it.sourceVerse == verse) ||
            (it.linkedVolume == volume && it.linkedBook == book && it.linkedChapter == chapter && it.linkedVerse == verse)
        }
    }

    // ================== HIGHLIGHTS ==================
    
    private fun getHighlightsFile(context: Context): File {
        return File(context.filesDir, HIGHLIGHTS_FILENAME)
    }

    fun getAllHighlights(context: Context): List<Highlight> {
        val file = getHighlightsFile(context)
        if (!file.exists()) return emptyList()
        
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<Highlight>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveHighlight(context: Context, highlight: Highlight) {
        val highlights = getAllHighlights(context).toMutableList()
        highlights.removeAll { it.id == highlight.id }
        highlights.add(highlight)
        saveHighlightsInternal(context, highlights)
    }

    fun deleteHighlight(context: Context, highlightId: String) {
        val highlights = getAllHighlights(context).toMutableList()
        highlights.removeAll { it.id == highlightId }
        saveHighlightsInternal(context, highlights)
    }

    private fun saveHighlightsInternal(context: Context, highlights: List<Highlight>) {
        try {
            val jsonString = gson.toJson(highlights)
            getHighlightsFile(context).writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHighlightsForVerse(context: Context, volume: String, book: String, chapter: Int, verse: Int): List<Highlight> {
        return getAllHighlights(context).filter {
            it.volume == volume && it.book == book && it.chapter == chapter && it.verse == verse
        }
    }
}
