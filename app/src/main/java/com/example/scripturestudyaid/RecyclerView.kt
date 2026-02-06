package com.example.scripturestudyaid // Update this to match your package

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VerseAdapter(
    private var verses: List<Verse>
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

    private var highlights: List<Highlight> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVerseText: TextView = view.findViewById(R.id.tvVerseText)
    }

    private var previewHighlight: Highlight? = null

    fun setPreviewHighlight(highlight: Highlight?) {
        this.previewHighlight = highlight
        notifyDataSetChanged()
    }

    // Callback for gestures: (Verse, Highlight?) -> Unit.
    // If Highlight is null, it's a long press (create). If not null, it's a tap on existing (edit).
    private var onVerseInteraction: ((Verse, Highlight?) -> Unit)? = null

    fun setOnVerseInteractionListener(listener: (Verse, Highlight?) -> Unit) {
        this.onVerseInteraction = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verse, parent, false)
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

        // Helper to apply span
        fun applyHighlight(h: Highlight) {
            val start = h.startOne + textOffset
            val end = h.endOne + textOffset
            if (start >= 0 && end <= fullString.length && start < end) {
                if (h.type == "UNDERLINE") {
                    spannable.setSpan(
                        UnderlineSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        ForegroundColorSpan(h.color),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    spannable.setSpan(
                        android.text.style.BackgroundColorSpan(h.color),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        for (highlight in verseHighlights) {
            applyHighlight(highlight)
        }

        // Apply Preview if matches this verse
        if (previewHighlight?.verse == verse.verse) {
            applyHighlight(previewHighlight!!)
        }

        holder.tvVerseText.text = spannable

        holder.tvVerseText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.main_menu, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.action_highlight -> {
                        val selStart = holder.tvVerseText.selectionStart
                        val selEnd = holder.tvVerseText.selectionEnd

                        // Adjust for verse number
                        val start = selStart - textOffset
                        val end = selEnd - textOffset

                        val newHighlight = Highlight(0, verse.verse, "", start, end, 0, "", "") // Dummy highlight
                        onVerseInteraction?.invoke(verse, newHighlight)
                        mode?.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
            }
        }
    }

    override fun getItemCount() = verses.size

    fun updateData(newVerses: List<Verse>, newHighlights: List<Highlight>) {
        this.verses = newVerses
        this.highlights = newHighlights
        notifyDataSetChanged()
    }
}