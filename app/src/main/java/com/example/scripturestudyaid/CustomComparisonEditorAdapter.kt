package com.example.scripturestudyaid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomComparisonEditorAdapter(
    private var pairs: MutableList<CustomComparisonPair>,
    private val onAddVerseClick: (position: Int, isLeft: Boolean) -> Unit,
    private val onDeleteRowClick: (position: Int) -> Unit,
    private val onClearCellClick: (position: Int, isLeft: Boolean) -> Unit
) : RecyclerView.Adapter<CustomComparisonEditorAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLeftVerse: TextView = view.findViewById(R.id.tvLeftVerse)
        val tvLeftText: TextView = view.findViewById(R.id.tvLeftText)
        val btnAddLeft: ImageButton = view.findViewById(R.id.btnAddLeft)
        val btnClearLeft: ImageButton = view.findViewById(R.id.btnClearLeft)
        
        val tvRightVerse: TextView = view.findViewById(R.id.tvRightVerse)
        val tvRightText: TextView = view.findViewById(R.id.tvRightText)
        val btnAddRight: ImageButton = view.findViewById(R.id.btnAddRight)
        val btnClearRight: ImageButton = view.findViewById(R.id.btnClearRight)

        val btnDeleteRow: ImageButton = view.findViewById(R.id.btnDeleteRow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_comparison_editor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pair = pairs[position]

        // LEFT SIDE
        if (pair.left.verseReference.isNotEmpty()) {
            holder.tvLeftVerse.text = pair.left.verseReference
            holder.tvLeftText.text = pair.left.text
            holder.tvLeftVerse.visibility = View.VISIBLE
            holder.tvLeftText.visibility = View.VISIBLE
            holder.btnAddLeft.visibility = View.GONE
            holder.btnClearLeft.visibility = View.VISIBLE
        } else {
            holder.tvLeftVerse.visibility = View.GONE
            holder.tvLeftText.visibility = View.GONE
            holder.btnAddLeft.visibility = View.VISIBLE
            holder.btnClearLeft.visibility = View.GONE
        }

        holder.btnAddLeft.setOnClickListener { onAddVerseClick(holder.adapterPosition, true) }
        holder.btnClearLeft.setOnClickListener { onClearCellClick(holder.adapterPosition, true) }

        // RIGHT SIDE
        if (pair.right.verseReference.isNotEmpty()) {
            holder.tvRightVerse.text = pair.right.verseReference
            holder.tvRightText.text = pair.right.text
            holder.tvRightVerse.visibility = View.VISIBLE
            holder.tvRightText.visibility = View.VISIBLE
            holder.btnAddRight.visibility = View.GONE
            holder.btnClearRight.visibility = View.VISIBLE
        } else {
            holder.tvRightVerse.visibility = View.GONE
            holder.tvRightText.visibility = View.GONE
            holder.btnAddRight.visibility = View.VISIBLE
            holder.btnClearRight.visibility = View.GONE
        }

        holder.btnAddRight.setOnClickListener { onAddVerseClick(holder.adapterPosition, false) }
        holder.btnClearRight.setOnClickListener { onClearCellClick(holder.adapterPosition, false) }

        // ROW DELETE
        holder.btnDeleteRow.setOnClickListener { onDeleteRowClick(holder.adapterPosition) }
    }

    override fun getItemCount() = pairs.size
}
