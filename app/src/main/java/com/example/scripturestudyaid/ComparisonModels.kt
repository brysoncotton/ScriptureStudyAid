package com.example.scripturestudyaid

import java.io.Serializable

data class ComparisonItem(
    val source1Verse: String,
    val source1Text: String,
    val source2Verse: String,
    val source2Text: String
)

data class ComparisonConfig(
    val title: String,
    val filename: String,
    val source1Name: String, // e.g., "Genesis"
    val source2Name: String, // e.g., "Moses"
    val customComparisonId: String? = null
) : Serializable

data class SearchResult(
    val volume: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val verseText: String
)
