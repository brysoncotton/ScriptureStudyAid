package com.example.scripturestudyaid

import android.content.Context
import com.google.gson.Gson
import java.io.IOException

object JsonUtils {
    fun getScriptures(context: Context, fileName: String): ScriptureResponse? {
        val jsonString: String
        try {
            // This opens the requested JSON file
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }

        // Special handling for Doctrine and Covenants
        if (fileName.contains("doctrine-and-covenants")) {
            val dcData = Gson().fromJson(jsonString, DcResponse::class.java)
            
            // Map DC Sections to standard Chapters
            val chapters = dcData.sections.map { section ->
                Chapter(
                    chapter = section.section,
                    reference = section.reference,
                    verses = section.verses
                )
            }
            
            // Create a single "Book"
            val dcBook = Book(
                book = "Doctrine and Covenants",
                chapters = chapters
            )
            
            return ScriptureResponse(books = listOf(dcBook))
        }

        // This turns the String of text into our ScriptureResponse object
        return Gson().fromJson(jsonString, ScriptureResponse::class.java)
    }

    fun getComparisonData(context: Context, filename: String, source1Key: String, source2Key: String): List<ComparisonItem>? {
        val jsonString: String
        try {
            jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }

        val gson = Gson()
        val list = mutableListOf<ComparisonItem>()

        try {
            // Parse as a list of Maps to handle dynamic keys
            val rawList = gson.fromJson(jsonString, List::class.java) as List<Map<String, Any>>

            for (item in rawList) {
                // Check if it's the new format (Source 1/2 Verse)
                // Note: The keys in the JSON are "Source 1 Reference" and "Source 2 reference" based on the file inspection
                val verse1Raw = item["Source 1 Verse"] ?: item["Source 1 Reference"]
                val verse2Raw = item["Source 2 Verse"] ?: item["Source 2 reference"]
                
                // Format verse numbers as strings
                val v1 = formatVerse(verse1Raw)
                val v2 = formatVerse(verse2Raw)

                val text1 = item[source1Key] as? String ?: ""
                val text2 = item[source2Key] as? String ?: ""

                list.add(ComparisonItem(v1, text1, v2, text2))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return list
    }

    private fun formatVerse(verseObj: Any?): String {
        return when (verseObj) {
            is Double -> verseObj.toInt().toString() // Gson parses numbers as Doubles
            is String -> verseObj
            else -> ""
        }
    }
}