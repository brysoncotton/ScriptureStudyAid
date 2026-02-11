package com.example.scripturestudyaid

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SearchEngine {

    private val volumes = mapOf(
        "Old Testament" to "old-testament.json",
        "New Testament" to "new-testament.json",
        "Book of Mormon" to "book-of-mormon.json",
        "Doctrine and Covenants" to "doctrine-and-covenants.json",
        "Pearl of Great Price" to "pearl-of-great-price.json"
    )

    // Cache to avoid re-parsing JSONs constantly
    private var cachedScriptures: MutableMap<String, ScriptureResponse> = mutableMapOf()

    suspend fun getWordFrequency(context: Context, query: String): Map<String, Int> = withContext(Dispatchers.IO) {
        ensureDataLoaded(context)
        val frequencyMap = mutableMapOf<String, Int>()
        val lowerQuery = query.lowercase()

        cachedScriptures.values.forEach { response ->
            response.books.forEach { book ->
                var count = 0
                book.chapters.forEach { chapter ->
                    chapter.verses.forEach { verse ->
                        // Count occurrences in this verse
                        count += countOccurrences(verse.text.lowercase(), lowerQuery)
                    }
                }
                
                // Aggregate by book name
                // If a book appears in multiple volumes (unlikely in this dataset but good practice), sum it up
                frequencyMap[book.book] = frequencyMap.getOrDefault(book.book, 0) + count
            }
        }
        // Filter out books with 0 count to keep chart clean? Or keep them to show absence?
        // Let's keep only non-zero for now to avoid clutter
        frequencyMap.filter { it.value > 0 }
    }

    suspend fun getWordFrequencies(context: Context, queries: List<String>): Map<String, Map<String, Int>> = withContext(Dispatchers.IO) {
        ensureDataLoaded(context)
        val frequencyMap = mutableMapOf<String, MutableMap<String, Int>>()
        val lowerQueries = queries.map { it.lowercase() }

        cachedScriptures.values.forEach { response ->
            response.books.forEach { book ->
                val bookCounts = frequencyMap.getOrPut(book.book) { mutableMapOf() }
                
                book.chapters.forEach { chapter ->
                    chapter.verses.forEach { verse ->
                        val text = verse.text.lowercase()
                        lowerQueries.forEachIndexed { index, query ->
                             val count = countOccurrences(text, query)
                             if (count > 0) {
                                 val originalQuery = queries[index] // Key by original case or lowercase? Let's use original for display
                                 bookCounts[originalQuery] = bookCounts.getOrDefault(originalQuery, 0) + count
                             }
                        }
                    }
                }
            }
        }
        // Filter out books with 0 hits across all queries?
        frequencyMap.filter { entry -> entry.value.values.sum() > 0 }
    }

    suspend fun getChapterFrequency(context: Context, bookName: String, query: String): Map<String, Int> = withContext(Dispatchers.IO) {
        ensureDataLoaded(context)
        val frequencyMap = mutableMapOf<String, Int>()
        val lowerQuery = query.lowercase()

        cachedScriptures.values.forEach { response ->
            response.books.filter { it.book == bookName }.forEach { book ->
                book.chapters.forEach { chapter ->
                    var count = 0
                    chapter.verses.forEach { verse ->
                        count += countOccurrences(verse.text.lowercase(), lowerQuery)
                    }
                    // Key is just chapter number string "1", "2"
                    val key = chapter.chapter.toString()
                    frequencyMap[key] = frequencyMap.getOrDefault(key, 0) + count
                }
            }
        }
        frequencyMap.filter { it.value > 0 }
    }

    suspend fun getChapterFrequencies(context: Context, bookName: String, queries: List<String>): Map<String, Map<String, Int>> = withContext(Dispatchers.IO) {
        ensureDataLoaded(context)
        val frequencyMap = mutableMapOf<String, MutableMap<String, Int>>() // Chapter -> (Term -> Count)
        val lowerQueries = queries.map { it.lowercase() }

        cachedScriptures.values.forEach { response ->
            response.books.filter { it.book == bookName }.forEach { book ->
                book.chapters.forEach { chapter ->
                    val chapterKey = chapter.chapter.toString()
                    val chapterCounts = frequencyMap.getOrPut(chapterKey) { mutableMapOf() }
                    
                    chapter.verses.forEach { verse ->
                        val text = verse.text.lowercase()
                        lowerQueries.forEachIndexed { index, query ->
                            val count = countOccurrences(text, query)
                            if (count > 0) {
                                val originalQuery = queries[index]
                                chapterCounts[originalQuery] = chapterCounts.getOrDefault(originalQuery, 0) + count
                            }
                        }
                    }
                }
            }
        }
        frequencyMap.filter { entry -> entry.value.values.sum() > 0 }
    }

    suspend fun proximitySearch(
        context: Context, 
        term1: String, 
        term2: String, 
        maxDistance: Int
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        ensureDataLoaded(context)
        val results = mutableListOf<SearchResult>()
        val t1 = term1.lowercase()
        val t2 = term2.lowercase()

        // Iterate through all volumes/books/chapters/verses
        volumes.forEach { (volumeName, _) ->
            val response = cachedScriptures[volumeName] ?: return@forEach
            response.books.forEach { book ->
                book.chapters.forEach { chapter ->
                    chapter.verses.forEach { verse ->
                        val text = verse.text.lowercase()
                        // Optimization: check if both terms exist in the verse text at all first
                        if (text.contains(t1) && text.contains(t2)) {
                            if (checkProximity(text, t1, t2, maxDistance)) {
                                results.add(
                                    SearchResult(
                                        volume = volumeName,
                                        book = book.book,
                                        chapter = chapter.chapter,
                                        verse = verse.verse,
                                        verseText = verse.text
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        results
    }

    private suspend fun ensureDataLoaded(context: Context) {
        if (cachedScriptures.size == volumes.size) return

        volumes.forEach { (name, filename) ->
            if (!cachedScriptures.containsKey(name)) {
                val data = JsonUtils.getScriptures(context, filename)
                if (data != null) {
                    cachedScriptures[name] = data
                }
            }
        }
    }

    private fun countOccurrences(text: String, query: String): Int {
        var count = 0
        var index = 0
        while (true) {
            index = text.indexOf(query, index)
            if (index == -1) break
            count++
            index += query.length
        }
        return count
    }

    private fun checkProximity(text: String, term1: String, term2: String, maxDistance: Int): Boolean {
        // Split text into words, removing punctuation (simple approach)
        val words = text.split(Regex("[\\s\\p{Punct}]+"))
        
        val indices1 = mutableListOf<Int>()
        val indices2 = mutableListOf<Int>()

        // Find all indices (word positions) for both terms
        words.forEachIndexed { index, word ->
            if (word == term1) indices1.add(index)
            if (word == term2) indices2.add(index)
        }

        // Check distance between every pair of occurrences
        for (i1 in indices1) {
            for (i2 in indices2) {
                if (kotlin.math.abs(i1 - i2) <= maxDistance) {
                    return true
                }
            }
        }
        return false
    }
}
