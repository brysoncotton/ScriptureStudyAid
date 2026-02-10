package com.example.scripturestudyaid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomComparisonListAdapter(
    private var comparisons: List<CustomComparison>,
    private val onItemClick: (CustomComparison) -> Unit,
    private val onItemEditClick: (CustomComparison) -> Unit,
    private val onItemDeleteClick: (CustomComparison) -> Unit
) : RecyclerView.Adapter<CustomComparisonListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvComparisonTitle)
        val btnEdit: android.widget.ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: android.widget.ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_comparison_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = comparisons[position]
        holder.tvTitle.text = item.title
        
        holder.itemView.setOnClickListener { onItemClick(item) }
        
        // Edit Button (Use dedicated callback or re-purpose long click? Let's add explicit callback)
        holder.btnEdit.setOnClickListener { 
            // We need a callback for edit. Currently using onItemLongClick for delete/edit?
            // The constructor signature needs update or we reuse existing params.
            // Let's assume we update constructor signature or use a new interface.
            // For now, let's call onItemLongClick as placeholder or fix the class signature.
            // BETTER: Fix class signature to include onEditClick and onDeleteClick separately.
            onItemEditClick(item)
        }
        
        holder.btnDelete.setOnClickListener { 
            onItemDeleteClick(item)
        }
    }

    override fun getItemCount() = comparisons.size

    fun updateList(newList: List<CustomComparison>) {
        comparisons = newList
        notifyDataSetChanged()
    }
}
