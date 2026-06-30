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
    private val onEditClick: (ContactItem) -> Unit,   // أكشن التعديل
    private val onDeleteClick: (ContactItem) -> Unit  // أكشن الحذف
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    // الـ ViewHolder يستقبل الـ Binding مباشرة بدلاً من الـ View التقليدي
    class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        // نفخ (Inflate) التصميم باستخدام الـ View Binding
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactsList[position]

        // استخدام الـ binding الداخلي للـ holder للوصول لكل العناصر بـ Type-safe
        holder.binding.apply {
            contactNameTv.text = contact.name
            relationshipTv.text = contact.relationship
            phoneTv.text = contact.phone

            // إظهار أو إخفاء شارة الأهمية
            priorityBadge.visibility = if (contact.isPriority) View.VISIBLE else View.GONE

            // 📝 تشغيل زرار التعديل
            editContactBtn.setOnClickListener {
                onEditClick(contact)
            }

            // 🗑️ تشغيل زرار الحذف
            deleteContactBtn.setOnClickListener {
                onDeleteClick(contact)
            }

            // 📞 تشغيل زرار الاتصال الفوري الخاص بكِ
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