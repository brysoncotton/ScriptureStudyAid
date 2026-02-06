package com.example.scripturestudyaid

import android.graphics.Color

data class Note(
    val id: String,
    val volume: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val selectionStart: Int, // Index relative to the Verse.text
    val selectionEnd: Int,
    val selectedText: String,
    val noteContent: String,
    val timestamp: Long,
    val color: Int = Color.parseColor("#FFFACD")
)
