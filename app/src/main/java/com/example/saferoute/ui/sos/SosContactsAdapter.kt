package com.example.saferoute.ui.sos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.databinding.ItemSosContactHorizontalBinding
import com.example.saferoute.models.ContactItem

class SosContactsAdapter(
    private val contacts: MutableList<ContactItem>
) : RecyclerView.Adapter<SosContactsAdapter.SosViewHolder>() {


    class SosViewHolder(val binding: ItemSosContactHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SosViewHolder {
        val binding = ItemSosContactHorizontalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SosViewHolder(binding)
    }
    var onContactRemoved: ((MutableList<ContactItem>) -> Unit)? = null
    override fun onBindViewHolder(holder: SosViewHolder, position: Int) {
        val contact = contacts[position]
        holder.binding.tvContactName.text = contact.name

        holder.binding.btnRemoveContact.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val itemToRemove = contacts[currentPosition]
                onContactRemoved?.invoke(mutableListOf(itemToRemove))
            }
        }
    }

    override fun getItemCount(): Int = contacts.size
    fun updateList(newList: List<ContactItem>) {
        this.contacts.clear()
        this.contacts.addAll(newList)
        notifyDataSetChanged()
    }
}