package com.example.scripturestudyaid

import androidx.room.Entity
import androidx.room.PrimaryKey

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
