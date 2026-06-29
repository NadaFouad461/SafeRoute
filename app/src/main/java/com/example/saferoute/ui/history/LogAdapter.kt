package com.example.saferoute.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.example.saferoute.data.local.EmergencyLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<EmergencyLog, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvLogIcon)
        private val tvType: TextView = itemView.findViewById(R.id.tvLogType)
        private val tvTime: TextView = itemView.findViewById(R.id.tvLogTime)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLogLocation)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvLogStatus)

        fun bind(log: EmergencyLog) {
            tvType.text = log.type
            tvLocation.text = "${log.latitude}, ${log.longitude}"


            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            tvTime.text = sdf.format(Date(log.timestamp))


            when (log.type.uppercase()) {
                "SOS" -> {
                    tvIcon.text = "⚠️"
                    tvStatus.text = "Resolved"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                }
                "SAFE WALK" -> {
                    tvIcon.text = "👣"
                    tvStatus.text = "Completed"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                }
                "FALL DETECTED" -> {
                    tvIcon.text = "📉"
                    tvStatus.text = "False Alarm"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                }
                else -> {
                    tvIcon.text = "📌"
                    tvStatus.text = log.status
                }
            }
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<EmergencyLog>() {
        override fun areItemsTheSame(oldItem: EmergencyLog, newItem: EmergencyLog): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EmergencyLog, newItem: EmergencyLog): Boolean = oldItem == newItem
    }
}