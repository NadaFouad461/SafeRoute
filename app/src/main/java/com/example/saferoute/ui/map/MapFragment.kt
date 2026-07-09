package com.example.saferoute.ui.map

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.data.repository.LocationRepository
import com.example.saferoute.databinding.FragmentMapBinding
import com.example.saferoute.services.LocationTrackingService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale
import kotlin.concurrent.thread

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()

    private lateinit var marker: Marker
    private lateinit var accuracyCircle: Polygon

    private var currentPoint: GeoPoint? = null
    private var firstLocationReceived = true

    private var isDemoMode = false
    private val handler = Handler(Looper.getMainLooper())

    private var nearestDangerPoint: GeoPoint? = null

    private data class DangerZone(val point: GeoPoint, val description: String)
    private val dangerZones = mutableListOf<DangerZone>()
    private val dangerZoneMarkers = mutableListOf<Marker>()
    private var dangerZonesListener: ValueEventListener? = null
    private val dangerZonesRef = FirebaseDatabase.getInstance().getReference("dangerZones")

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("lon", 0.0) ?: 0.0

            _binding?.let { safeBinding ->
                val point = GeoPoint(lat, lon)
                currentPoint = point

                safeBinding.txtLat.text = "Lat: %.5f".format(point.latitude)
                safeBinding.txtLon.text = "Lon: %.5f".format(point.longitude)

                viewModel.updateLocation(point)
                updateNearestDangerZone(point)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        Configuration.getInstance().userAgentValue = requireContext().packageName
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        val map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomNavigation()
        updateEmergencyContactsCount()

        binding.shareLocationBtnCard.setOnClickListener {
            if (LocationRepository.isSharingLocation) {
                LocationRepository.stopSharing()
                binding.txtShareTitle.text = "Share Live Location"
                binding.shareLocationBtnCard.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#4FC3F7"))
                )
                Toast.makeText(requireContext(), "🔴 تم إيقاف مشاركة الموقع", Toast.LENGTH_SHORT).show()
            } else {
                LocationRepository.isSharingLocation = true
                currentPoint?.let { LocationRepository.updateLocation(it.latitude, it.longitude) }
                binding.txtShareTitle.text = "Sharing..."
                binding.shareLocationBtnCard.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#22C55E"))
                )
                Toast.makeText(requireContext(), "📡 جاري مشاركة موقعك", Toast.LENGTH_SHORT).show()
            }
        }

        binding.searchIcon.setOnClickListener {
            val searchQuery = binding.searchSafeDestinationsEt.text.toString().trim()
            if (searchQuery.isNotEmpty()) {
                performMapSearch(searchQuery)
            } else {
                Toast.makeText(requireContext(), "Please enter a place to search", Toast.LENGTH_SHORT).show()
            }
        }

        binding.searchSafeDestinationsEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val searchQuery = binding.searchSafeDestinationsEt.text.toString().trim()
                if (searchQuery.isNotEmpty()) performMapSearch(searchQuery)
                true
            } else false
        }

        binding.btnMyLocation.setOnClickListener {
            currentPoint?.let {
                binding.map.controller.animateTo(it)
                binding.map.controller.setZoom(19.0)
            }
        }

        binding.btnViewRisk.setOnClickListener {
            nearestDangerPoint?.let {
                binding.map.controller.animateTo(it)
                binding.map.controller.setZoom(19.0)
            }
        }

        binding.map.controller.setZoom(5.0)
        binding.map.controller.setCenter(GeoPoint(26.8206, 30.8025))

        showLastKnownLocation()
        startListeningToDangerZones()

        // Handle navigation arguments (e.g. from History)
        val argsLat = arguments?.getDouble("latitude").takeIf { it != null && it != 0.0 }
            ?: arguments?.getDouble("lat", 0.0) ?: 0.0
        val argsLon = arguments?.getDouble("longitude").takeIf { it != null && it != 0.0 }
            ?: arguments?.getDouble("lon")?.takeIf { it != 0.0 }
            ?: arguments?.getDouble("lng", 0.0) ?: 0.0

        if (argsLat != 0.0 && argsLon != 0.0) {
            val historyPoint = GeoPoint(argsLat, argsLon)
            currentPoint = historyPoint
            firstLocationReceived = false
            binding.map.controller.setCenter(historyPoint)
            binding.map.controller.setZoom(18.5)

            val emergencyMarker = Marker(binding.map).apply {
                position = historyPoint
                title = "🚨 Emergency Incident Location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
            }
            binding.map.overlays.add(emergencyMarker)
        }

        viewModel.currentLocation.observe(viewLifecycleOwner) { point ->
            binding.progressLocation.visibility = View.GONE

            if (!::marker.isInitialized) {
                marker = Marker(binding.map).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "My Location"
                    position = point
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
                }
                binding.map.overlays.add(marker)

                accuracyCircle = Polygon().apply {
                    points = Polygon.pointsAsCircle(point, 50.0)
                    fillPaint.color = 0x1A534AB7.toInt()
                    outlinePaint.color = 0xFF534AB7.toInt()
                    outlinePaint.strokeWidth = 3f
                }
                binding.map.overlays.add(accuracyCircle)
            } else {
                marker.position = point
                accuracyCircle.points = Polygon.pointsAsCircle(point, 30.0)
            }

            if (firstLocationReceived) {
                binding.map.controller.setCenter(point)
                binding.map.controller.setZoom(19.0)
                firstLocationReceived = false
                addCustomPointsOfInterest(point)
            }

            binding.map.invalidate()
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(locationReceiver, IntentFilter("LocationUpdateIntent"))

        checkAndRequestLocationPermission()

        binding.btnDemo.setOnClickListener {
            if (isDemoMode) {
                stopFakeMovement()
                startTrackingService()
                binding.btnDemo.text = "Start Demo"
            } else {
                stopTrackingService()
                startFakeMovement()
                binding.btnDemo.text = "Stop Demo"
            }
        }
    }

    private fun startTrackingService() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopTrackingService() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java)
        requireContext().stopService(intent)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.selectedItemId = R.id.nav_map
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_map) return@setOnItemSelectedListener true
            try {
                when (item.itemId) {
                    R.id.nav_home -> { findNavController().navigate(R.id.homeFragment); true }
                    R.id.nav_history -> { findNavController().navigate(R.id.action_mapFragment_to_historyFragment); true }
                    R.id.nav_contacts -> { findNavController().navigate(R.id.contactsListFragment); true }
                    R.id.nav_profile -> { findNavController().navigate(R.id.profileFragment2); true }
                    else -> false
                }
            } catch (e: Exception) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "مسار التنقل غير مدعوم حالياً", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }

    private fun updateEmergencyContactsCount() {
        val contactsCount = 3
        binding.txtContacts.text = "WITH $contactsCount EMERGENCY CONTACTS"
    }

    private fun performMapSearch(query: String) {
        binding.progressLocation.visibility = View.VISIBLE
        thread {
            try {
                val geocoder = android.location.Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocationName(query, 5)

                activity?.runOnUiThread {
                    binding.progressLocation.visibility = View.GONE
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val searchPoint = GeoPoint(address.latitude, address.longitude)

                        binding.map.controller.animateTo(searchPoint)
                        binding.map.controller.setZoom(16.0)

                        val searchMarker = Marker(binding.map).apply {
                            position = searchPoint
                            title = query
                            snippet = address.getAddressLine(0)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        binding.map.overlays.add(searchMarker)
                        binding.map.invalidate()
                        Toast.makeText(requireContext(), "Found: $query", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Location not found, try another name", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    binding.progressLocation.visibility = View.GONE
                    Toast.makeText(requireContext(), "Search error or no internet connection", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addCustomPointsOfInterest(userPoint: GeoPoint) {
        val map = binding.map
        val hospitalPoint = GeoPoint(userPoint.latitude + 0.0012, userPoint.longitude + 0.0015)
        map.overlays.add(Marker(map).apply {
            position = hospitalPoint
            title = "🏥 مستشفى عام (نقطة طبية آمنة)"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_call)
        })

        val mosquePoint = GeoPoint(userPoint.latitude - 0.0015, userPoint.longitude - 0.0010)
        map.overlays.add(Marker(map).apply {
            position = mosquePoint
            title = "🕌 مسجد المنطقة الرئيسي"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces)
        })

        val policePoint = GeoPoint(userPoint.latitude + 0.0008, userPoint.longitude - 0.0018)
        map.overlays.add(Marker(map).apply {
            position = policePoint
            title = "👮 مركز شرطة / نقطة أمنية"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_info)
        })
        map.invalidate()
    }

    private fun startListeningToDangerZones() {
        if (dangerZonesListener != null) return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                dangerZones.clear()
                dangerZoneMarkers.forEach { binding.map.overlays.remove(it) }
                dangerZoneMarkers.clear()

                for (zone in snapshot.children) {
                    val lat = zone.child("latitude").getValue(Double::class.java) ?: continue
                    val lon = zone.child("longitude").getValue(Double::class.java) ?: continue
                    val desc = zone.child("description").getValue(String::class.java) ?: "Danger Zone"
                    val point = GeoPoint(lat, lon)
                    dangerZones.add(DangerZone(point, desc))

                    val marker = Marker(binding.map).apply {
                        position = point
                        title = desc
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_danger_zone)
                    }
                    dangerZoneMarkers.add(marker)
                    binding.map.overlays.add(marker)
                }
                binding.map.invalidate()
                updateNearestDangerZone(currentPoint)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dangerZonesListener = listener
        dangerZonesRef.addValueEventListener(listener)
    }

    private fun updateNearestDangerZone(user: GeoPoint?) {
        if (_binding == null) return
        if (user == null || dangerZones.isEmpty()) {
            binding.highRiskCard.visibility = View.GONE
            nearestDangerPoint = null
            return
        }

        var closestZone: DangerZone? = null
        var closestDistance = Float.MAX_VALUE
        val result = FloatArray(1)

        for (zone in dangerZones) {
            android.location.Location.distanceBetween(
                user.latitude, user.longitude,
                zone.point.latitude, zone.point.longitude,
                result
            )
            if (result[0] < closestDistance) {
                closestDistance = result[0]
                closestZone = zone
            }
        }

        if (closestZone != null && closestDistance <= 200f) {
            nearestDangerPoint = closestZone.point
            binding.highRiskCard.visibility = View.VISIBLE
            binding.txtRiskDescription.text = "${closestZone.description} • ${closestDistance.toInt()} m away"
        } else {
            binding.highRiskCard.visibility = View.GONE
            nearestDangerPoint = null
        }
    }

    private fun checkAndRequestLocationPermission() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startTrackingService()
        } else {
            requestPermissions(permissions.toTypedArray(), LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && _binding != null) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrackingService()
                showLastKnownLocation()
            }
        }
    }

    private fun showLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val lastKnown = lastGps ?: lastNet
        lastKnown?.let {
            val point = GeoPoint(it.latitude, it.longitude)
            currentPoint = point
            viewModel.updateLocation(point)
        }
    }

    private fun startFakeMovement() {
        isDemoMode = true
        var lat = 30.3346471
        var lon = 31.7504029
        handler.post(object : Runnable {
            override fun run() {
                if (!isDemoMode || _binding == null) return
                lat += 0.0001
                lon += 0.0001
                val point = GeoPoint(lat, lon)
                currentPoint = point
                binding.txtLat.text = "Lat: %.5f".format(point.latitude)
                binding.txtLon.text = "Lon: %.5f".format(point.longitude)

                if (!::marker.isInitialized) {
                    marker = Marker(binding.map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Demo Location"
                        position = point
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
                    }
                    binding.map.overlays.add(marker)
                } else {
                    marker.position = point
                    if (::accuracyCircle.isInitialized) accuracyCircle.points = Polygon.pointsAsCircle(point, 25.0)
                }
                viewModel.updateLocation(point)
                binding.map.controller.setCenter(point)
                binding.map.invalidate()
                handler.postDelayed(this, 1500)
            }
        })
    }

    private fun stopFakeMovement() {
        isDemoMode = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isDemoMode = false
        handler.removeCallbacksAndMessages(null)
        stopTrackingService()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(locationReceiver)
        dangerZonesListener?.let { dangerZonesRef.removeEventListener(it) }
        dangerZonesListener = null
        _binding = null
    }
}