package com.example.saferoute.ui.contacts

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.databinding.ItemContactBinding // تأكدي أن الاسم مطابق لملف الـ XML بتاعك
import com.example.saferoute.models.ContactItem

class ContactsAdapter(
    private val contactsList: List<ContactItem>,
    private val onEditClick: (ContactItem) -> Unit,
    private val onDeleteClick: (ContactItem) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {


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
        val contact = contactsList[position]


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

    override fun getItemCount(): Int = contactsList.size
}