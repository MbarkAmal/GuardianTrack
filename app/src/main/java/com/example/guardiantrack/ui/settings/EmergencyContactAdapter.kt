package com.example.guardiantrack.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guardiantrack.data.model.EmergencyContactEntity
import com.example.guardiantrack.databinding.ItemContactBinding

class EmergencyContactAdapter(
    private val onDeleteClick: (EmergencyContactEntity) -> Unit
) : ListAdapter<EmergencyContactEntity, EmergencyContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnDeleteContact.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(adapterPosition))
                }
            }
        }

        fun bind(contact: EmergencyContactEntity) {
            binding.tvContactName.text = contact.name
            binding.tvContactPhone.text = contact.phoneNumber
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<EmergencyContactEntity>() {
        override fun areItemsTheSame(oldItem: EmergencyContactEntity, newItem: EmergencyContactEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EmergencyContactEntity, newItem: EmergencyContactEntity): Boolean {
            return oldItem == newItem
        }
    }
}
