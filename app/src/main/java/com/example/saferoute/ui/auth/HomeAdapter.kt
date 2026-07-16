package com.example.saferoute.ui.auth

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.example.saferoute.databinding.ItemHomeFragmentBinding // تأكدي من استيراد كلاس الـ Binding الصحيح

class HomeAdapter(
    private val activities: MutableList<HomeItem>
) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHomeFragmentBinding)
        : RecyclerView.ViewHolder(binding.root)

    fun updateList(newList: List<HomeItem>) {
        activities.clear()
        activities.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = ItemHomeFragmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = activities[position]

        holder.binding.titleTv.text = item.title
        holder.binding.timeTv.text = item.timestamp

        when (item.type.lowercase()) {
            "resolved" -> {
                holder.binding.iconContainer.setCardBackgroundColor(
                    Color.parseColor("#DCFCE7")
                )
                holder.binding.activityIcon.setImageResource(
                    R.drawable.ic_check_circle
                )
                holder.binding.activityIcon.setColorFilter(
                    Color.parseColor("#10B981")
                )
            }
            "sos" -> {
                holder.binding.iconContainer.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                holder.binding.activityIcon.setImageResource(android.R.drawable.ic_delete)
                holder.binding.activityIcon.setColorFilter(Color.parseColor("#EF4444"))
            }
            "fake_call" -> {
                holder.binding.iconContainer.setCardBackgroundColor(Color.parseColor("#E0F2FE"))
                holder.binding.activityIcon.setImageResource(android.R.drawable.sym_action_call)
                holder.binding.activityIcon.setColorFilter(Color.parseColor("#3B82F6"))
            }
            "safe_walk" -> {
                holder.binding.iconContainer.setCardBackgroundColor(Color.parseColor("#DCFCE7"))
                holder.binding.activityIcon.setImageResource(android.R.drawable.ic_menu_directions)
                holder.binding.activityIcon.setColorFilter(Color.parseColor("#10B981"))
            }
            else -> {
                holder.binding.iconContainer.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                holder.binding.activityIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                holder.binding.activityIcon.setColorFilter(Color.parseColor("#64748B"))
            }
        }
    }

    override fun getItemCount() = activities.size
}