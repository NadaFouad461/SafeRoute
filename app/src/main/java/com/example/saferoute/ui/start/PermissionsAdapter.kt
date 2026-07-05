package com.example.saferoute.ui.start

import PermissionModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.google.android.material.switchmaterial.SwitchMaterial

class PermissionsAdapter(
    private val items: List<PermissionModel>,
    private val onSwitchCheckedChange: (PermissionModel, Boolean) -> Unit
) : RecyclerView.Adapter<PermissionsAdapter.PermissionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_permission_card, parent, false)
        return PermissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onSwitchCheckedChange)
    }

    override fun getItemCount(): Int = items.size

    class PermissionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivBanner: ImageView = view.findViewById(R.id.ivPermissionBanner)
        private val ivIcon: ImageView = view.findViewById(R.id.ivPermissionIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvPermissionTitle)
        private val tvDesc: TextView = view.findViewById(R.id.tvPermissionDesc)
        private val switchPerm: SwitchMaterial = view.findViewById(R.id.switchPermission)

        fun bind(item: PermissionModel, onCheckedChange: (PermissionModel, Boolean) -> Unit) {
            tvTitle.text = item.title
            tvDesc.text = item.description
            ivIcon.setImageResource(item.iconRes)
            ivBanner.setImageResource(item.bannerRes)


            switchPerm.setOnCheckedChangeListener(null)
            switchPerm.isChecked = item.isGranted

            switchPerm.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(item, isChecked)
            }
        }
    }
}