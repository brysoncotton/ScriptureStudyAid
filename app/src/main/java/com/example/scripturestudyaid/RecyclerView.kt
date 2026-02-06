package com.example.scripturestudyaid // Update this to match your package

import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VerseAdapter(
    private var verses: List<Verse>,
    private val onHighlightSelected: (Verse, Int, Int) -> Unit
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

    private var highlights: List<Highlight> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVerseText: TextView = view.findViewById(R.id.tvVerseText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val verse = verses[position]
        val fullString = "${verse.verse} ${verse.text}"
        val spannable = SpannableString(fullString)

        // Set the verse number to superscript
        val endOfNum = verse.verse.toString().length
        spannable.setSpan(SuperscriptSpan(), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(RelativeSizeSpan(0.7f), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Apply highlights
        val verseHighlights = highlights.filter { it.verse == verse.verse }
        val textOffset = endOfNum + 1
        for (highlight in verseHighlights) {
            val start = highlight.startOne + textOffset
            val end = highlight.endOne + textOffset
            if (start >= 0 && end <= fullString.length && start < end) {
                if (highlight.type == "UNDERLINE") {
                    spannable.setSpan(
                        ColoredUnderlineSpan(highlight.color),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    spannable.setSpan(
                        android.text.style.BackgroundColorSpan(highlight.color),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        holder.tvVerseText.text = spannable
        holder.tvVerseText.setTextIsSelectable(true)
        holder.tvVerseText.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.menu_highlight, menu)
                return true
            }

            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean {
                 if (item?.itemId == R.id.action_highlight) {
                    val start = holder.tvVerseText.selectionStart
                    val end = holder.tvVerseText.selectionEnd
                    
                    if (start >= 0 && end > start) {
                        // Adjust for verse number prefix
                        val validStart = (start - textOffset).coerceAtLeast(0)
                        val validEnd = (end - textOffset).coerceAtMost(verse.text.length)
                        
                        if (validEnd > validStart) {
                            onHighlightSelected(verse, validStart, validEnd)
                        }
                    }
                    mode?.finish()
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }
    }

    override fun getItemCount() = verses.size

    fun updateData(newVerses: List<Verse>, newHighlights: List<Highlight>) {
        this.verses = newVerses
        this.highlights = newHighlights
        notifyDataSetChanged()
    }
}