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

class VerseAdapter(private var verses: List<Verse>) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

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

        holder.tvVerseText.text = spannable
    }

    override fun getItemCount() = verses.size

    fun updateVerses(newVerses: List<Verse>) {
        this.verses = newVerses
        notifyDataSetChanged()
    }
}