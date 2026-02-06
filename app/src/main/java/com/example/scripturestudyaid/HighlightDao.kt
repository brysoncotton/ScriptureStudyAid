package com.example.scripturestudyaid

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HighlightDao {
    @Insert
    fun insert(highlight: Highlight)

    @Delete
    fun delete(highlight: Highlight)

    @Query("SELECT * FROM highlights WHERE volume = :volume AND book = :book AND chapter = :chapter AND verse = :verse")
    fun getHighlightsForVerse(volume: String, book: String, chapter: Int, verse: Int): List<Highlight>

    @Query("SELECT * FROM highlights WHERE volume = :volume AND book = :book AND chapter = :chapter")
    fun getHighlightsForChapter(volume: String, book: String, chapter: Int): List<Highlight>
}
