package com.example.scripturestudyaid

import androidx.room.Entity
import androidx.room.PrimaryKey

// The user was receiving a "no such table" error from KSP.
// By adding this comment, we are forcing KSP to re-process this file.
@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val volume: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val startOne: Int,
    val endOne: Int,
    val color: Int,
    val type: String = "SOLID" // "SOLID" or "UNDERLINE"
)
