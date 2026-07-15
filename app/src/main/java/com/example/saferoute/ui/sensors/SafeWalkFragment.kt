package com.example.saferoute.ui.sensors


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.data.repository.LocationRepository
import com.example.saferoute.databinding.FragmentSafeWalkBinding
import com.example.saferoute.services.LocationTrackingService
import com.example.saferoute.services.SafeWalkService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint
class SafeWalkFragment : Fragment(R.layout.fragment_safe_walk) {

    private var _binding: FragmentSafeWalkBinding? = null
    private val binding get() = _binding!!

    private lateinit var myMarker: Marker
    private var firstLocationReceived = true
    val currentBatteryLevel: Int
        get() {
            val batteryManager =
                requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }


    // بيستقبل تحديثات الموقع اللايف اللي بتتبعت من LocationTrackingService
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("lon", 0.0) ?: 0.0
            if (lat == 0.0 && lon == 0.0) return
            updateMiniMap(GeoPoint(lat, lon))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSafeWalkBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupMiniMap()
        binding.tvBattery.text = "🔋 $currentBatteryLevel%"

        if (!SafeWalkService.isWalkActive.value) {
            val duration = arguments?.getInt("duration") ?: 20
            val serviceIntent = Intent(requireContext(), SafeWalkService::class.java).apply {
                putExtra("DURATION_MINUTES", duration)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }
        }

        // 2. مراقبة العداد التنازلي وتحديث الشاشة (Real-time)
        viewLifecycleOwner.lifecycleScope.launch {
            SafeWalkService.timeRemaining.collect { timeString ->
                binding.tvTripDuration.text = timeString
            }
        }

        setupButtons()
    }

    private fun setupMiniMap() {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        binding.miniMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.miniMap.setMultiTouchControls(true)
        binding.miniMap.controller.setZoom(17.0)
        binding.miniMap.controller.setCenter(
            GeoPoint(
                26.8206,
                30.8025
            )
        ) // مركز افتراضي لحد ما نجيب موقع حقيقي

        LocationRepository.getLastKnownLocation()?.let { point ->
            updateMiniMap(point)
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(locationReceiver, IntentFilter("LocationUpdateIntent"))

        startLocationUpdates()

        binding.tvExpandMap.setOnClickListener {
            try {
                findNavController().navigate(R.id.mapFragment)
            } catch (_: Exception) {
            }
        }
    }

    private fun updateMiniMap(point: GeoPoint) {
        if (_binding == null) return

        if (!::myMarker.isInitialized) {
            myMarker = Marker(binding.miniMap).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "موقعك الحالي"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
            }
            binding.miniMap.overlays.add(myMarker)
        }
        myMarker.position = point

        if (firstLocationReceived) {
            binding.miniMap.controller.setCenter(point)
            firstLocationReceived = false
        }
        binding.miniMap.invalidate()

        LocationRepository.updateLocation(point.latitude, point.longitude)
    }

    private fun startLocationUpdates() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val intent = Intent(requireContext(), LocationTrackingService::class.java)
            ContextCompat.startForegroundService(requireContext(), intent)
        }
    }

    private fun setupButtons() {
        // أ. المستخدم وصل بالسلامة أو عايز ينهي الرحلة
        val endTripAction = View.OnClickListener {
            stopServiceAndExit(triggerSOS = false)
        }
        binding.btnArrivedSafely.setOnClickListener(endTripAction)
        binding.btnEndTrip.setOnClickListener(endTripAction)

        // ب. زرار الطوارئ الفوري (لو حس بخطر وعايز يضرب الإنذار فوراً)
        binding.cardFeelingUnsafe.setOnClickListener {
            stopServiceAndExit(triggerSOS = true)
        }
    }

    private fun stopServiceAndExit(triggerSOS: Boolean) {
        // 1. نوقف الخدمة والعداد
        requireContext().stopService(Intent(requireContext(), SafeWalkService::class.java))

        // 2. نحدد الخطوة الجاية
        if (triggerSOS) {
            // نفتح شاشة الإنذار الأحمر
            val intent = Intent(requireContext(), FallAlertActivity::class.java)
            startActivity(intent)
        } else {
            // نرجع للشاشة الرئيسية (الهوم)
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.miniMap?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.miniMap?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(locationReceiver)
        requireContext().stopService(Intent(requireContext(), LocationTrackingService::class.java))
        _binding = null
    }
}
