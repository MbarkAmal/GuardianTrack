package com.example.guardiantrack.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.guardiantrack.databinding.ItemHistoryBinding
import com.example.guardiantrack.data.model.IncidentEntity
import android.content.Intent
import android.net.Uri
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
        
        // Bind location
        val locationText = if (!item.address.isNullOrBlank()) {
            item.address
        } else if (item.latitude != 0.0 || item.longitude != 0.0) {
            "Lat: ${"%.4f".format(item.latitude)}, Lon: ${"%.4f".format(item.longitude)}"
        } else {
            "Localisation non disponible"
        }
        holder.binding.tvIncidentLocation.text = locationText

        // Handle click to open Google Maps
        holder.binding.layoutLocation.setOnClickListener {
            if (item.latitude != 0.0 || item.longitude != 0.0) {
                val gmmIntentUri = Uri.parse("geo:${item.latitude},${item.longitude}?q=${item.latitude},${item.longitude}(${item.type})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                // Start activity safely
                try {
                    holder.itemView.context.startActivity(mapIntent)
                } catch (e: Exception) {
                    // Fallback for browsers if Maps app is missing
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${item.latitude},${item.longitude}"))
                    holder.itemView.context.startActivity(browserIntent)
                }
            }
        }
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