package com.example.scripturestudyaid

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object NotesRepository {
    private const val FILENAME = "notes.json"
    private val gson = Gson()

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILENAME)
    }

    fun getAllNotes(context: Context): List<Note> {
        val file = getFile(context)
        if (!file.exists()) return emptyList()
        
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<Note>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveNote(context: Context, note: Note) {
        val notes = getAllNotes(context).toMutableList()
        notes.add(note)
        saveNotesInternal(context, notes)
    }

    fun deleteNote(context: Context, noteId: String) {
        val notes = getAllNotes(context).toMutableList()
        notes.removeAll { it.id == noteId }
        saveNotesInternal(context, notes)
    }

    private fun saveNotesInternal(context: Context, notes: List<Note>) {
        try {
            val jsonString = gson.toJson(notes)
            getFile(context).writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNotesForVerse(context: Context, volume: String, book: String, chapter: Int, verse: Int): List<Note> {
        return getAllNotes(context).filter {
            it.volume == volume && it.book == book && it.chapter == chapter && it.verse == verse
        }
    }
}
