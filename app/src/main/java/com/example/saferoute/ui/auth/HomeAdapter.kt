package com.example.saferoute.ui.auth

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.example.saferoute.ui.auth.HomeItem

class HomeAdapter(private val activities: List<HomeItem>) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTv: TextView = view.findViewById(R.id.titleTv)
        val timeTv: TextView = view.findViewById(R.id.timeTv)
        val activityIcon: ImageView = view.findViewById(R.id.activityIcon)
        val iconContainer: CardView = view.findViewById(R.id.iconContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_fragment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = activities[position]
        holder.titleTv.text = item.title
        holder.timeTv.text = item.timestamp


        when (item.type.lowercase()) {
            "sos" -> {
                holder.iconContainer.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                holder.activityIcon.setImageResource(android.R.drawable.ic_delete)
                holder.activityIcon.setColorFilter(Color.parseColor("#EF4444"))
            }
            "fake_call" -> {
                holder.iconContainer.setCardBackgroundColor(Color.parseColor("#E0F2FE"))
                holder.activityIcon.setImageResource(android.R.drawable.sym_action_call)
                holder.activityIcon.setColorFilter(Color.parseColor("#3B82F6"))
            }
            "safe_walk" -> {
                holder.iconContainer.setCardBackgroundColor(Color.parseColor("#DCFCE7"))
                holder.activityIcon.setImageResource(android.R.drawable.ic_menu_directions)
                holder.activityIcon.setColorFilter(Color.parseColor("#10B981"))
            }
            else -> {
                holder.iconContainer.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                holder.activityIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                holder.activityIcon.setColorFilter(Color.parseColor("#64748B"))
            }
        }
    }

    override fun getItemCount() = activities.size
}