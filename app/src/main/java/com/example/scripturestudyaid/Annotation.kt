package com.example.scripturestudyaid

import android.graphics.Color

// Tag that can be searched across all scriptures
data class Tag(
    val id: String,
    val tagName: String,
    val volume: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val timestamp: Long
)

// Simple bookmark marker
data class Bookmark(
    val id: String,
    val volume: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val timestamp: Long
)

// Link between scriptures
data class Link(
    val id: String,
    val sourceVolume: String,
    val sourceBook: String,
    val sourceChapter: Int,
    val sourceVerse: Int,
    val linkedVolume: String,
    val linkedBook: String,
    val linkedChapter: Int,
    val linkedVerse: Int,
    val timestamp: Long
)

// Unified annotation type for highlights (existing Note model handles notes)
data class Highlight(
    val id: String,
    val volume: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val selectionStart: Int,
    val selectionEnd: Int,
    val selectedText: String,
    val color: Int = Color.parseColor("#FFFACD"),
    val timestamp: Long
)
