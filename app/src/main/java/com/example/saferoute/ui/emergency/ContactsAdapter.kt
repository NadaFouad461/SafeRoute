package com.example.saferoute.ui.emergency

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.databinding.ItemContactBinding
import com.example.saferoute.models.ContactItem

class ContactsAdapter(
    private val onEditClick: (ContactItem) -> Unit,
    private val onDeleteClick: (ContactItem) -> Unit
) : ListAdapter<ContactItem, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)

        holder.binding.apply {
            contactNameTv.text = contact.name
            relationshipTv.text = contact.relationship
            phoneTv.text = contact.phone

            priorityBadge.visibility = if (contact.isPriority) View.VISIBLE else View.GONE

            editContactBtn.setOnClickListener {
                onEditClick(contact)
            }

            deleteContactBtn.setOnClickListener {
                onDeleteClick(contact)
            }

            callNowBtn.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contact.phone}")
                }
                root.context.startActivity(intent)
            }
        }
    }

    private class ContactDiffCallback : DiffUtil.ItemCallback<ContactItem>() {
        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            return oldItem == newItem
        }
    }
}
