package com.example.scripturestudyaid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomComparisonListAdapter(
    private var comparisons: List<CustomComparison>,
    private val onItemClick: (CustomComparison) -> Unit,
    private val onItemLongClick: (CustomComparison) -> Unit
) : RecyclerView.Adapter<CustomComparisonListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = comparisons[position]
        holder.tvTitle.text = item.title
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount() = comparisons.size

    fun updateList(newList: List<CustomComparison>) {
        comparisons = newList
        notifyDataSetChanged()
    }
}
