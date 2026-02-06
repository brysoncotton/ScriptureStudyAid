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

    fun getCreationComparison(context: Context): List<CreationComparisonItem>? {
        val jsonString: String
        try {
            jsonString = context.assets.open("creationAccountsComparison.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }

        // Parse list of creation items
        return Gson().fromJson(jsonString, Array<CreationComparisonItem>::class.java).toList()
    }
}