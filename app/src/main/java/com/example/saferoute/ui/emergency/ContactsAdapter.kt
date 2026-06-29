package com.example.saferoute.ui.contacts

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.example.saferoute.models.ContactItem

class ContactsAdapter(private val contactsList: List<ContactItem>) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contactNameTv: TextView = view.findViewById(R.id.contactNameTv)
        val relationshipTv: TextView = view.findViewById(R.id.relationshipTv)
        val phoneTv: TextView = view.findViewById(R.id.phoneTv)
        val priorityBadge: TextView = view.findViewById(R.id.priorityBadge)
        val callNowBtn: Button = view.findViewById(R.id.callNowBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactsList[position]

        holder.contactNameTv.text = contact.name
        holder.relationshipTv.text = contact.relationship
        holder.phoneTv.text = contact.phone


        if (contact.isPriority) {
            holder.priorityBadge.visibility = View.VISIBLE
        } else {
            holder.priorityBadge.visibility = View.GONE
        }


        holder.callNowBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contact.phone}")
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = contactsList.size
}