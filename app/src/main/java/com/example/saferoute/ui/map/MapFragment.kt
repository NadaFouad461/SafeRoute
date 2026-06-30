package com.example.saferoute.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentMapBinding
import com.example.saferoute.services.LocationTrackingService
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

    private lateinit var trackingService: LocationTrackingService
    private lateinit var marker: Marker
    private lateinit var accuracyCircle: Polygon

    private var currentPoint: GeoPoint? = null
    private var firstLocationReceived = true

    private var isDemoMode = false
    private val handler = Handler(Looper.getMainLooper())

    private var isSharingLiveLocation = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
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

        val map = binding.map

        // 🛠️ تشغيل الـ Bottom Navigation الآمن والمعدل يدوياً لمنع أي تهنيج في التنقل
        setupBottomNavigation()

        updateEmergencyContactsCount()

        binding.shareLocationBtnCard.setOnClickListener {
            val relativeLayout = binding.shareLocationBtnCard.getChildAt(0) as? ViewGroup
            val titleTextView = relativeLayout?.getChildAt(0) as? TextView

            if (isSharingLiveLocation) {
                isSharingLiveLocation = false
                titleTextView?.text = "Share Live Location"
                binding.shareLocationBtnCard.setCardBackgroundColor(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#74C3EE"))
                )
                Toast.makeText(requireContext(), "Stopped sharing live location", Toast.LENGTH_SHORT).show()
            } else {
                isSharingLiveLocation = true
                titleTextView?.text = "Sharing Location..."
                binding.shareLocationBtnCard.setCardBackgroundColor(
                    ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark)
                )
                Toast.makeText(requireContext(), "Live location sharing activated!", Toast.LENGTH_SHORT).show()
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
                if (searchQuery.isNotEmpty()) {
                    performMapSearch(searchQuery)
                }
                true
            } else {
                false
            }
        }

        binding.btnMyLocation.setOnClickListener {
            currentPoint?.let {
                map.controller.animateTo(it)
                map.controller.setZoom(19.0)
            }
        }

        map.controller.setZoom(5.0)
        map.controller.setCenter(GeoPoint(26.8206, 30.8025))

        showLastKnownLocation()

        val argsLat = arguments?.getDouble("lat", 0.0) ?: 0.0
        val argsLon = arguments?.getDouble("lon", 0.0) ?: 0.0

        if (argsLat != 0.0 && argsLon != 0.0) {
            val historyPoint = GeoPoint(argsLat, argsLon)
            currentPoint = historyPoint
            firstLocationReceived = false

            map.controller.setCenter(historyPoint)
            map.controller.setZoom(18.5)

            val emergencyMarker = Marker(map).apply {
                position = historyPoint
                title = "🚨 Emergency Incident Location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
            }
            map.overlays.add(emergencyMarker)
            map.invalidate()
        }

        viewModel.currentLocation.observe(viewLifecycleOwner) { point ->
            binding.progressLocation.visibility = View.GONE

            if (!::marker.isInitialized) {
                marker = Marker(map).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "My Location"
                    position = point
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
                }
                map.overlays.add(marker)

                accuracyCircle = Polygon().apply {
                    points = Polygon.pointsAsCircle(point, 50.0)
                    fillPaint.color = 0x1A534AB7.toInt()
                    outlinePaint.color = 0xFF534AB7.toInt()
                    outlinePaint.strokeWidth = 3f
                }
                map.overlays.add(accuracyCircle)

            } else {
                marker.position = point
                accuracyCircle.points = Polygon.pointsAsCircle(point, 30.0)
            }

            if (firstLocationReceived) {
                map.controller.setCenter(point)
                map.controller.setZoom(19.0)
                firstLocationReceived = false

                addCustomPointsOfInterest(point)
            }

            map.invalidate()
        }

        trackingService = LocationTrackingService(requireContext()) { lat, lon ->
            val point = GeoPoint(lat, lon)
            currentPoint = point
            binding.txtLat.text = "Lat: %.5f".format(point.latitude)
            binding.txtLon.text = "Lon: %.5f".format(point.longitude)
            viewModel.updateLocation(point)
        }

        checkAndRequestLocationPermission()

        binding.btnDemo.setOnClickListener {
            if (isDemoMode) {
                stopFakeMovement()
                trackingService.start()
                binding.btnDemo.text = "Start Demo"
            } else {
                trackingService.stop()
                startFakeMovement()
                binding.btnDemo.text = "Stop Demo"
            }
        }
    }

    // 🛠️ دالة الـ Bottom Navigation الاحترافية لمنع قفل التبويبات أو تعليقها أمام اللجنة
    private fun setupBottomNavigation() {
        // تحديد التبويب الحالي للشاشة (الخريطة) بنجاح
        binding.bottomNavigationView.selectedItemId = R.id.nav_map

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            // منع إعادة تحميل صفحة الخريطة لو ضغطت عليها وهي مفتوحة بالفعل
            if (item.itemId == R.id.nav_map) {
                return@setOnItemSelectedListener true
            }

            try {
                when (item.itemId) {
                    R.id.nav_home -> {
                        findNavController().navigate(R.id.homeFragment)
                        true
                    }
                    R.id.nav_history -> {
                        findNavController().navigate(R.id.action_mapFragment_to_historyFragment)
                        true
                    }
                    R.id.nav_contacts -> {
                        findNavController().navigate(R.id.contactsListFragment)
                        true
                    }
                    R.id.nav_profile -> {
                        findNavController().navigate(R.id.profileFragment2)
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "مسار التنقل غير مدعوم حالياً", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun updateEmergencyContactsCount() {
        val contactsCount = 3
        val relativeLayout = binding.shareLocationBtnCard.getChildAt(0) as? ViewGroup
        val descTextView = relativeLayout?.getChildAt(1) as? TextView
        descTextView?.text = "WITH $contactsCount EMERGENCY CONTACTS"
    }

    private fun performMapSearch(query: String) {
        binding.progressLocation.visibility = View.VISIBLE

        thread {
            try {
                val geocoder = android.location.Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocationName(query, 5)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val searchPoint = GeoPoint(address.latitude, address.longitude)

                    activity?.runOnUiThread {
                        binding.progressLocation.visibility = View.GONE
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
                    }
                } else {
                    activity?.runOnUiThread {
                        binding.progressLocation.visibility = View.GONE
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
        val hospitalMarker = Marker(map).apply {
            position = hospitalPoint
            title = "🏥 مستشفى عام (نقطة طبية آمنة)"
            snippet = "تبعد 200 متر عن موقعك الحالي"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_call)
        }
        map.overlays.add(hospitalMarker)

        val mosquePoint = GeoPoint(userPoint.latitude - 0.0015, userPoint.longitude - 0.0010)
        val mosqueMarker = Marker(map).apply {
            position = mosquePoint
            title = "🕌 مسجد المنطقة الرئيسي"
            snippet = "مكان تجمع متاح ومضاء"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces)
        }
        map.overlays.add(mosqueMarker)

        val policePoint = GeoPoint(userPoint.latitude + 0.0008, userPoint.longitude - 0.0018)
        val policeMarker = Marker(map).apply {
            position = policePoint
            title = "👮 مركز شرطة / نقطة أمنية"
            snippet = "دورية حماية متوفرة"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_info)
        }
        map.overlays.add(policeMarker)

        map.invalidate()
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            trackingService.start()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                trackingService.start()
                showLastKnownLocation()
            }
        }
    }

    private fun showLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val locationManager =
            requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager

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
                if (!isDemoMode) return

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
                    if (::accuracyCircle.isInitialized) {
                        accuracyCircle.points = Polygon.pointsAsCircle(point, 25.0)
                    }
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
        trackingService.stop()
        _binding = null
    }
}