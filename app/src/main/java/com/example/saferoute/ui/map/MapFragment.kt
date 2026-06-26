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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentMapBinding
import com.example.saferoute.services.LocationTrackingService
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

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

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

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

        binding.btnMyLocation.setOnClickListener {

            currentPoint?.let {

                map.controller.animateTo(it)
                map.controller.setZoom(18.0)

            }
        }


        map.controller.setZoom(5.0)
        map.controller.setCenter(GeoPoint(26.8206, 30.8025)) // مصر كنقطة بداية


        showLastKnownLocation()

        // Observe ViewModel


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

                //
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

            map.controller.setCenter(point)
            map.controller.setZoom(18.0)
            map.invalidate()
        }



        trackingService = LocationTrackingService(requireContext()) { lat, lon ->
            val point = GeoPoint(lat, lon)
            currentPoint = point
            binding.txtLat.text =
                "Lat: %.5f".format(point.latitude)

            binding.txtLon.text =
                "Lon: %.5f".format(point.longitude)
            viewModel.updateLocation(point)
        }


        checkAndRequestLocationPermission()

        // Demo button
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

                if (!::marker.isInitialized) {
                    marker = Marker(binding.map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Demo Location"
                        position = point
                    }
                    binding.map.overlays.add(marker)
                } else {
                    marker.position = point
                    if (::accuracyCircle.isInitialized) {
                        accuracyCircle.points =
                            Polygon.pointsAsCircle(point, 25.0)
                    }
                }

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