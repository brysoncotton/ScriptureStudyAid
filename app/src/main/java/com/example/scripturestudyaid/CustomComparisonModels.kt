package com.example.scripturestudyaid

import java.util.UUID

data class CustomComparison(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val pairs: MutableList<CustomComparisonPair> = mutableListOf(),
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomComparisonPair(
    var left: CustomComparisonItem,
    var right: CustomComparisonItem
)

data class CustomComparisonItem(
    var verseReference: String = "", // e.g., "Genesis 1:1"
    var text: String = ""
)
