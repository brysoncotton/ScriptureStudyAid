package com.example.scripturestudyaid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ComparisonAdapter(
    private val items: List<ComparisonItem>,
    private val source1Name: String,
    private val source2Name: String
) : RecyclerView.Adapter<ComparisonAdapter.ComparisonViewHolder>() {

    class ComparisonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSource1Verse: TextView = itemView.findViewById(R.id.tvSource1Verse)
        val tvSource1Text: TextView = itemView.findViewById(R.id.tvSource1Text)
        val tvSource2Verse: TextView = itemView.findViewById(R.id.tvSource2Verse)
        val tvSource2Text: TextView = itemView.findViewById(R.id.tvSource2Text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComparisonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_creation_comparison, parent, false)
        return ComparisonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComparisonViewHolder, position: Int) {
        val item = items[position]
        
        // Source 1 Verse Label
        if (item.source1Verse.isNotEmpty()) {
            holder.tvSource1Verse.visibility = View.VISIBLE
            val text = item.source1Verse
            if (text.startsWith(source1Name, ignoreCase = true)) {
                holder.tvSource1Verse.text = text
            } else {
                holder.tvSource1Verse.text = "$source1Name $text"
            }
        } else {
            holder.tvSource1Verse.visibility = View.GONE
        }
        holder.tvSource1Text.text = formatText(item.source1Text)

        // Source 2 Verse Label
        if (item.source2Verse.isNotEmpty()) {
            holder.tvSource2Verse.visibility = View.VISIBLE
            val text = item.source2Verse
            if (text.startsWith(source2Name, ignoreCase = true)) {
                holder.tvSource2Verse.text = text
            } else {
                holder.tvSource2Verse.text = "$source2Name $text"
            }
        } else {
            holder.tvSource2Verse.visibility = View.GONE
        }
        holder.tvSource2Text.text = formatText(item.source2Text)
    }

    private fun formatText(text: String): CharSequence {
        if (!text.contains("<italic>")) {
            return text
        }

        // Simple parsing: replace <italic>...</italic> sections
        // We use SpannableStringBuilder to apply styles
        val builder = android.text.SpannableStringBuilder()
        
        var currentIndex = 0
        while (currentIndex < text.length) {
            val startTagIndex = text.indexOf("<italic>", currentIndex)
            if (startTagIndex == -1) {
                // No more tags, append the rest
                builder.append(text.substring(currentIndex))
                break
            }

            // Append text before the tag
            builder.append(text.substring(currentIndex, startTagIndex))

            val endTagIndex = text.indexOf("</italic>", startTagIndex)
            if (endTagIndex == -1) {
                // malformed? just append the rest including the tag?
                builder.append(text.substring(startTagIndex))
                break
            }

            // Extract content inside tags
            val content = text.substring(startTagIndex + 8, endTagIndex)
            
            val startSpan = builder.length
            builder.append(content)
            val endSpan = builder.length

            // Apply Italic style
            builder.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                startSpan,
                endSpan,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            // Apply Lighter Color (Gray)
            builder.setSpan(
                android.text.style.ForegroundColorSpan(android.graphics.Color.GRAY),
                startSpan,
                endSpan,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            currentIndex = endTagIndex + 9 // length of </italic>
        }
        
        return builder
    }

    override fun getItemCount() = items.size
}
