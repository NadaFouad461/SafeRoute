package com.example.saferoute.ui.history

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemHistoryLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemHistoryLogBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: EmergencyLog) {
            binding.tvLogType.text = log.type

            val calculatedAddress = getAddressName(itemView.context, log.latitude, log.longitude)
            binding.tvLogLocation.text = calculatedAddress

            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.tvLogTime.text = sdf.format(Date(log.timestamp))

            // 🎯 تفكيك الـ status وقراءة العدد الديناميكي المحدث في الفايرستور
            val statusParts = log.status.split("|")
            val actualStatus = statusParts.getOrNull(0) ?: "Dispatched"
            val contactsCount = statusParts.getOrNull(1) ?: "3"

            // ربط مباشر بـ tvContactsAlerted المكتوب في الـ XML بتاعك
            binding.tvContactsAlerted.text = "$contactsCount Contacts alerted"

            when (log.type.uppercase()) {
                "SOS" -> {
                    binding.tvLogIcon.text = "⚠️"

                    if (actualStatus.contains("Dispatched", ignoreCase = true)) {
                        binding.tvLogStatus.text = "Emergency Dispatched"
                        binding.tvLogStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                        binding.tvLogStatus.setBackgroundResource(R.drawable.bg_dispatched_badge)
                        binding.tvLogStatus.setPadding(32, 12, 32, 12)
                    } else {
                        binding.tvLogStatus.text = "Resolved"
                        binding.tvLogStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                        binding.tvLogStatus.setBackgroundResource(0)
                        binding.tvLogStatus.setPadding(0, 0, 0, 0)
                    }
                }

                "SAFE WALK" -> {
                    binding.tvLogIcon.text = "👣"
                    binding.tvLogStatus.text = "Completed"
                    binding.tvLogStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                    binding.tvLogStatus.setBackgroundResource(0)
                    binding.tvLogStatus.setPadding(0, 0, 0, 0)
                }

                "FALL DETECTED" -> {
                    binding.tvLogIcon.text = "📉"
                    binding.tvLogStatus.text = "False Alarm"
                    binding.tvLogStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                    binding.tvLogStatus.setBackgroundResource(0)
                    binding.tvLogStatus.setPadding(0, 0, 0, 0)
                }

                else -> {
                    binding.tvLogIcon.text = "📌"
                    binding.tvLogStatus.text = actualStatus
                    binding.tvLogStatus.setBackgroundResource(0)
                    binding.tvLogStatus.setPadding(0, 0, 0, 0)
                }
            }

            binding.locationContainer.setOnClickListener {
                val bundle = Bundle().apply {
                    putDouble("lat", log.latitude)
                    putDouble("lon", log.longitude)
                }
                try {
                    itemView.findNavController().navigate(R.id.mapFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(itemView.context, "جاري الانتقال لموقع الاستغاثة...", Toast.LENGTH_SHORT).show()
                }
            }

            binding.tvViewDetail.setOnClickListener {
                val bundle = Bundle().apply {
                    putInt("logId", log.id)
                    putString("locationName", calculatedAddress)
                    putInt("batteryLevel", log.batteryLevel)
                    putInt("contactsCount", contactsCount.toIntOrNull() ?: 3)
                    putDouble("lat", log.latitude)
                    putDouble("lon", log.longitude)
                    putString("logType", log.type)
                }
                try {
                    itemView.findNavController().navigate(R.id.action_historyFragment_to_logDetailsFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(itemView.context, "جاري فتح التقرير...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun getAddressName(context: Context, lat: Double, lng: Double): String {
            if (lat == 0.0 && lng == 0.0) return "مصر، المنوفية، مدينة السادات"
            return try {
                val geocoder = Geocoder(context, Locale("ar"))
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    "${address.countryName ?: ""}, ${address.adminArea ?: ""}, ${address.subAdminArea ?: address.locality ?: ""}"
                } else {
                    "$lat, $lng"
                }
            } catch (e: Exception) {
                "مصر، المنوفية، السادات"
            }
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<EmergencyLog>() {
        override fun areItemsTheSame(oldItem: EmergencyLog, newItem: EmergencyLog): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EmergencyLog, newItem: EmergencyLog): Boolean = oldItem == newItem
    }
}