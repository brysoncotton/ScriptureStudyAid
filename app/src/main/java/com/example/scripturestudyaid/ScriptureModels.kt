package com.example.scripturestudyaid

data class ScriptureResponse(
    val books: List<Book>
)

data class Book(
    val book: String,
    val chapters: List<Chapter>
)

data class Chapter(
    val chapter: Int,
    val reference: String,
    val verses: List<Verse>
)

data class Verse(
    val reference: String,
    val text: String,
    val verse: Int
)

data class DcResponse(
    val sections: List<DcSection>
)

data class DcSection(
    val section: Int,
    val reference: String,
    val verses: List<Verse>
)

data class Highlight(
    val id: Int,
    val verse: Int,
    val type: String,
    val startOne: Int,
    val endOne: Int,
    val color: Int,
    val note: String,
    val date: String
)