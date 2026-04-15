package com.example.guardiantrack.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.guardiantrack.databinding.ItemHistoryBinding
import com.example.guardiantrack.data.model.IncidentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val list: MutableList<IncidentEntity>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class HistoryViewHolder(val binding: ItemHistoryBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = list[position]

        holder.binding.tvIncidentTitle.text = item.type
        holder.binding.tvIncidentDate.text = dateFormat.format(Date(item.timestamp))
        holder.binding.tvIncidentStatus.text = if (item.isSynced) "Synchronisé" else "En attente"
    }

    override fun getItemCount() = list.size

    fun getItem(position: Int): IncidentEntity {
        return list[position]
    }

    // for swipe delete
    fun removeItem(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)
    }
}