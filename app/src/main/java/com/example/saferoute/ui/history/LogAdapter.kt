package com.example.saferoute.ui.history

import android.content.Context
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.databinding.ItemHistoryLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<EmergencyLog, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    private var onItemClickListener: ((EmergencyLog) -> Unit)? = null
    private var onSafeClickListener: ((EmergencyLog) -> Unit)? = null

    fun setOnItemClickListener(listener: (EmergencyLog) -> Unit) { onItemClickListener = listener }
    fun setOnSafeClickListener(listener: (EmergencyLog) -> Unit) { onSafeClickListener = listener }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemHistoryLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClickListener, onSafeClickListener)
    }

    class LogViewHolder(private val binding: ItemHistoryLogBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: EmergencyLog, clickListener: ((EmergencyLog) -> Unit)?, safeClickListener: ((EmergencyLog) -> Unit)?) {

            binding.btnResolveAlert.setOnClickListener(null)
            binding.tvViewDetail.setOnClickListener(null)
            binding.locationContainer.setOnClickListener(null)

            val cleanType = log.type.split("|").getOrNull(0) ?: log.type
            val statusParts = log.status.split("|")
            val actualStatus = statusParts.getOrNull(0) ?: "Dispatched"
            val dynamicPayload = statusParts.getOrNull(1) ?: "0"

            if (actualStatus == "Incoming_SOS") {
                binding.tvLogType.text = "⚠️ INCOMING SOS"
                binding.tvLogIcon.text = "🚨"
                binding.tvLogStatus.text = "استغاثة نشطة من: $dynamicPayload"
                binding.tvLogStatus.setTextColor(Color.WHITE)
                binding.tvLogStatus.setBackgroundColor(Color.parseColor("#DC2626"))
                binding.tvLogStatus.setPadding(24, 12, 24, 12)
                binding.tvContactsAlerted.text = "اضغطي على View Details لتتبع الموقع فوراً"
                binding.btnResolveAlert.visibility = View.GONE
            } else {
                binding.tvLogType.text = cleanType
                binding.tvContactsAlerted.text = "$dynamicPayload Contacts alerted"

                when (cleanType.uppercase()) {
                    "SOS" -> {
                        if (actualStatus.contains("Dispatched", ignoreCase = true) || actualStatus.contains("MANUAL", ignoreCase = true) || actualStatus.contains("AUTO", ignoreCase = true) ||actualStatus.contains("Needs Help", ignoreCase = true)) {
                            binding.tvLogIcon.text = "⚠️"
                            binding.tvLogStatus.text = "Emergency Dispatched"
                            binding.tvLogStatus.setTextColor(Color.parseColor("#EF4444"))
                            binding.tvLogStatus.setBackgroundResource(R.drawable.bg_dispatched_badge)
                            binding.tvLogStatus.setPadding(24, 8, 24, 8)
                            binding.btnResolveAlert.visibility = View.VISIBLE
                            binding.btnResolveAlert.text = "I am safe now ✓"
                            binding.btnResolveAlert.setBackgroundColor(Color.parseColor("#10B981"))
                        } else {
                            binding.tvLogIcon.text = "✅"
                            binding.tvLogStatus.text = "Resolved"
                            binding.tvLogStatus.setTextColor(Color.parseColor("#10B981"))
                            binding.tvLogStatus.setBackgroundResource(0)
                            binding.tvLogStatus.setPadding(0, 0, 0, 0)
                            binding.btnResolveAlert.visibility = View.GONE
                        }
                    }
                    "SAFE WALK" -> {
                        binding.tvLogIcon.text = "👣"
                        binding.tvLogStatus.text = "Completed"
                        binding.tvLogStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                        binding.tvLogStatus.setBackgroundResource(0)
                        binding.btnResolveAlert.visibility = View.GONE
                    }
                    else -> {
                        binding.tvLogIcon.text = "📌"
                        binding.tvLogStatus.text = actualStatus
                        binding.tvLogStatus.setBackgroundResource(0)
                        binding.btnResolveAlert.visibility = View.GONE
                    }
                }
            }

            val calculatedAddress = getAddressName(itemView.context, log.latitude, log.longitude)
            binding.tvLogLocation.text = calculatedAddress
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.tvLogTime.text = sdf.format(Date(log.timestamp))


            binding.btnResolveAlert.setOnClickListener { safeClickListener?.invoke(log) }
            binding.tvViewDetail.setOnClickListener { clickListener?.invoke(log) }
            binding.locationContainer.setOnClickListener {
                val bundle = Bundle().apply {
                    putDouble("latitude", if (log.latitude == 0.0) 35.4344 else log.latitude)
                    putDouble("longitude", if (log.longitude == 0.0) 32.7457 else log.longitude)
                }
                itemView.findNavController().navigate(R.id.mapFragment, bundle)
            }
        }

        private fun getAddressName(context: Context, lat: Double, lng: Double): String {
            if (lat == 0.0 && lng == 0.0) return "مصر، المنوفية، مدينة السادات"
            return try {
                val geocoder = Geocoder(context, Locale("ar"))
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    "${address.countryName ?: ""}, ${address.adminArea ?: ""}, ${address.locality ?: ""}"
                } else { "$lat, $lng" }
            } catch (e: Exception) { "مصر، المنوفية، السادات" }
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<EmergencyLog>() {
        override fun areItemsTheSame(oldItem: EmergencyLog, newItem: EmergencyLog): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EmergencyLog, newItem: EmergencyLog): Boolean = oldItem == newItem
    }
}