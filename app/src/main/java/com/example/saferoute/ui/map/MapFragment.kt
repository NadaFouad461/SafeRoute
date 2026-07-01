package com.example.saferoute.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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

    private lateinit var trackingService: LocationTrackingService
    private lateinit var marker: Marker
    private lateinit var accuracyCircle: Polygon

    private var currentPoint: GeoPoint? = null
    private var nearestDangerPoint: GeoPoint? = null
    private var nearestDangerDescription = ""
    private var firstLocationReceived = true

    private var isDemoMode = false
    private val handler = Handler(Looper.getMainLooper())



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

        setupBottomNavigationPlaceholder()
        updateEmergencyContactsCount()


        binding.shareLocationBtnCard.setOnClickListener {
            val relativeLayout = binding.shareLocationBtnCard.getChildAt(0) as? ViewGroup
            val titleTextView = relativeLayout?.getChildAt(0) as? TextView

            if (LocationRepository.isSharingLocation) {
                LocationRepository.stopSharing()
                binding.txtShareTitle.text = "Share Live Location"
                binding.shareLocationBtnCard.setCardBackgroundColor(
                    ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#4FC3F7")
                    )
                )
                Toast.makeText(requireContext(), "🔴 تم إيقاف مشاركة الموقع", Toast.LENGTH_SHORT).show()
            } else {
                LocationRepository.isSharingLocation = true
                currentPoint?.let { LocationRepository.updateLocation(it.latitude, it.longitude) }
                binding.txtShareTitle.text = "Sharing..."
                binding.shareLocationBtnCard.setCardBackgroundColor(
                    ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#22C55E")
                    )
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
                map.controller.animateTo(it)
                map.controller.setZoom(19.0)
            }
        }

        map.controller.setZoom(5.0)
        map.controller.setCenter(GeoPoint(26.8206, 30.8025))

        showLastKnownLocation()
        loadDangerZones()
        binding.btnViewRisk.setOnClickListener {

            nearestDangerPoint?.let {

                binding.map.controller.animateTo(it)
                binding.map.controller.setZoom(19.0)

            }

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
            loadDangerZones()
            binding.txtLat.text = "Lat: %.5f".format(lat)
            binding.txtLon.text = "Lon: %.5f".format(lon)
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


    private fun setupBottomNavigationPlaceholder() {
        binding.bottomNavigationView.selectedItemId = R.id.nav_map
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_map) {
                true
            } else {

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
        map.overlays.add(Marker(map).apply {
            position = hospitalPoint
            title = "🏥 مستشفى عام (نقطة طبية آمنة)"
            snippet = "تبعد 200 متر عن موقعك الحالي"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_call)
        })

        val mosquePoint = GeoPoint(userPoint.latitude - 0.0015, userPoint.longitude - 0.0010)
        map.overlays.add(Marker(map).apply {
            position = mosquePoint
            title = "🕌 مسجد المنطقة الرئيسي"
            snippet = "مكان تجمع متاح ومضاء"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces)
        })

        val policePoint = GeoPoint(userPoint.latitude + 0.0008, userPoint.longitude - 0.0018)
        map.overlays.add(Marker(map).apply {
            position = policePoint
            title = "👮 مركز شرطة / نقطة أمنية"
            snippet = "دورية حماية متوفرة"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_info)
        })

        map.invalidate()
    }


    private fun loadDangerZones() {

        FirebaseDatabase.getInstance()
            .getReference("dangerZones")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    binding.map.overlays.removeAll {
                        it is Marker && it.icon ==
                                ContextCompat.getDrawable(requireContext(), R.drawable.ic_danger_zone)
                    }

                    binding.highRiskCard.visibility = View.GONE

                    val user = currentPoint

                    for (zone in snapshot.children) {

                        val lat = zone.child("latitude").getValue(Double::class.java) ?: continue
                        val lon = zone.child("longitude").getValue(Double::class.java) ?: continue
                        val desc = zone.child("description").getValue(String::class.java)
                            ?: "Danger Zone"

                        val point = GeoPoint(lat, lon)

                        val marker = Marker(binding.map).apply {

                            position = point
                            title = desc
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.ic_danger_zone
                            )
                        }

                        binding.map.overlays.add(marker)

                        if (user != null) {

                            val result = FloatArray(1)

                            android.location.Location.distanceBetween(
                                user.latitude,
                                user.longitude,
                                lat,
                                lon,
                                result
                            )

                            val distance = result[0]

                            if (distance <= 200f) {

                                nearestDangerPoint = point
                                nearestDangerDescription = desc

                                binding.highRiskCard.visibility = View.VISIBLE

                                binding.txtRiskDescription.text =
                                    "$desc • ${distance.toInt()} m away"

                            }

                        }

                    }

                    binding.map.invalidate()

                }

                override fun onCancelled(error: DatabaseError) {}
            })
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
            binding.txtLat.text = "Lat: %.5f".format(it.latitude)
            binding.txtLon.text = "Lon: %.5f".format(it.longitude)
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

                binding.txtLat.text = "Lat: %.5f".format(lat)
                binding.txtLon.text = "Lon: %.5f".format(lon)

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

                LocationRepository.updateLocation(lat, lon)
                binding.map.controller.setCenter(point)
                binding.map.controller.setZoom(18.0)
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