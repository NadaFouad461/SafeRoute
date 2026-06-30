package com.example.saferoute.ui.sos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.databinding.ItemSosContactHorizontalBinding
import com.example.saferoute.models.ContactItem

class SosContactsAdapter(
    private val contacts: MutableList<ContactItem>
) : RecyclerView.Adapter<SosContactsAdapter.SosViewHolder>() {

    // استخدام الـ View Binding داخل الـ ViewHolder مباشرة
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

    override fun onBindViewHolder(holder: SosViewHolder, position: Int) {
        val contact = contacts[position]

        // ربط البيانات بالـ Binding بدون findViewById
        holder.binding.tvContactName.text = contact.name

        // يمكنك هنا مستقبلاً ربط صورة حقيقية لو متوفرة في السيرفر
        // holder.binding.ivContactAvatar.load(contact.imageUrl)

        // عند الضغط على الـ (X) يتم حذف الحامي من قائمة الإرسال فوراً وتحديث الواجهة
        holder.binding.btnRemoveContact.setOnClickListener {
            // تأمين جلب الـ position الصحيح لتجنب كراش الحذف المتتالي
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                contacts.removeAt(currentPosition)
                notifyItemRemoved(currentPosition)
                notifyItemRangeChanged(currentPosition, contacts.size)
            }
        }
    }

    override fun getItemCount(): Int = contacts.size
}