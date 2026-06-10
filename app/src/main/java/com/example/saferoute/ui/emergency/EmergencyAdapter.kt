package com.example.saferoute.ui.emergency

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.databinding.ItemEmergencyLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmergencyAdapter(
    private var list: List<EmergencyLog>
) : RecyclerView.Adapter<EmergencyAdapter.MyViewHolder>() {

    class MyViewHolder(val binding: ItemEmergencyLogBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemEmergencyLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = list[position]


        holder.binding.typeText.text = item.type


        holder.binding.locationText.text =
            "Lat: ${item.latitude} | Lng: ${item.longitude}"

        val formattedDate = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date(item.timestamp))

        holder.binding.timeText.text = formattedDate
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<EmergencyLog>) {
        list = newList
        notifyDataSetChanged()
    }
}