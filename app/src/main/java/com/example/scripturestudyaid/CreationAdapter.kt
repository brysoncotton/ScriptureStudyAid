package com.example.scripturestudyaid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CreationAdapter(private val items: List<CreationComparisonItem>) :
    RecyclerView.Adapter<CreationAdapter.ValidationViewHolder>() {

    class ValidationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVerseNum: TextView = itemView.findViewById(R.id.tvVerseNum)
        val tvGenesis: TextView = itemView.findViewById(R.id.tvGenesis)
        val tvMoses: TextView = itemView.findViewById(R.id.tvMoses)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValidationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_creation_comparison, parent, false)
        return ValidationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ValidationViewHolder, position: Int) {
        val item = items[position]
        holder.tvVerseNum.text = "Verse ${item.Verse}"
        holder.tvGenesis.text = item.Genesis
        holder.tvMoses.text = item.Moses
    }

    override fun getItemCount() = items.size
}
