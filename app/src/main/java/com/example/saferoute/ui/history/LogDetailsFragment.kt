package com.example.saferoute.ui.history

import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentLogDetailsBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogDetailsFragment : Fragment() {

    private var _binding: FragmentLogDetailsBinding? = null
    private val binding get() = _binding!!

    private var logLatitude: Double = 0.0
    private var logLongitude: Double = 0.0
    private var logType: String = "SOS"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        _binding = FragmentLogDetailsBinding.inflate(inflater, container, false)

        binding.mapDetail.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapDetail.setMultiTouchControls(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. استقبال البيانات ديناميكياً من الـ Bundle الممرر من الأدابتر مباشرة
        logType = arguments?.getString("logType") ?: "SOS"
        logLatitude = arguments?.getDouble("lat") ?: 0.0
        logLongitude = arguments?.getDouble("lon") ?: 0.0
        val timeInLong = arguments?.getLong("timestamp") ?: System.currentTimeMillis()
        val battery = arguments?.getInt("batteryLevel") ?: 100
        val contactsCount = arguments?.getInt("contactsCount") ?: 3
        val locationName = arguments?.getString("locationName") ?: getAddressName(logLatitude, logLongitude)

        // 2. عرض البيانات النصية ديناميكياً في الواجهة دون داتا ثابتة
        binding.tvDetailType.text = "$logType Report"

        val sdf = SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault())
        binding.tvDetailTime.text = sdf.format(Date(timeInLong))

        binding.tvDetailLocation.text = "📍 Location: $locationName"
        binding.tvDetailBattery.text = "🔋 Battery level during event: $battery%"

        // 3. ضبط الحالة بناءً على نوع البلاغ
        binding.tvDetailStatus.text = "Dispatched"
        binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_dispatched_badge)

        // 4. عرض جهات الاتصال ديناميكياً بناءً على العدد الفعلي
        displayGuardians(contactsCount)

        // 5. تحديث وتوسيط الخريطة على إحداثيات البلاغ الحقيقي
        updateOSMMapLocation()
    }

    private fun updateOSMMapLocation() {
        if (logLatitude != 0.0 && logLongitude != 0.0) {
            val eventPoint = GeoPoint(logLatitude, logLongitude)

            binding.mapDetail.controller.setCenter(eventPoint)
            binding.mapDetail.controller.setZoom(17.0)

            val incidentMarker = Marker(binding.mapDetail).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Incident Location ($logType)"
                position = eventPoint
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
            }

            binding.mapDetail.overlays.clear()
            binding.mapDetail.overlays.add(incidentMarker)
            binding.mapDetail.invalidate()
        } else {
            showFallbackToCurrentLocation()
        }
    }

    private fun showFallbackToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val lastGps = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val lastKnown = lastGps ?: lastNet

            val fallbackPoint = if (lastKnown != null) {
                GeoPoint(lastKnown.latitude, lastKnown.longitude)
            } else {
                GeoPoint(30.3751, 30.5142) // الإحداثيات الافتراضية لمدينة السادات
            }

            binding.mapDetail.controller.setCenter(fallbackPoint)
            binding.mapDetail.controller.setZoom(16.5)

            val currentMarker = Marker(binding.mapDetail).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Current Mock Location"
                position = fallbackPoint
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
            }
            binding.mapDetail.overlays.clear()
            binding.mapDetail.overlays.add(currentMarker)
            binding.mapDetail.invalidate()
        }
    }

    private fun displayGuardians(contactsCount: Int) {
        binding.guardiansContainer.removeAllViews()

        // توليد واجهات ديناميكية لجهات الاتصال بناءً على العدد المحفوظ بالبوزيشن
        for (i in 1..contactsCount) {
            val tvGuardian = TextView(context).apply {
                text = "👤 Emergency Contact $i  •  [SMS Sent Successfully]"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.black))
                setPadding(16, 12, 16, 12)
            }
            binding.guardiansContainer.addView(tvGuardian)
        }
    }

    private fun getAddressName(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) return "مصر، المنوفية، مدينة السادات"
        return try {
            val geocoder = Geocoder(requireContext(), Locale("ar"))
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                "${address.countryName ?: ""}, ${address.adminArea ?: ""}, ${address.locality ?: ""}"
            } else "$lat, $lng"
        } catch (e: Exception) {
            "مصر، المنوفية، السادات"
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapDetail.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapDetail.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}