package com.example.saferoute.ui.emergency

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentEmergencyNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.concurrent.thread


@AndroidEntryPoint

class EmergencyNotificationFragment : Fragment(R.layout.fragment_emergency_notification) {

    private var _binding: FragmentEmergencyNotificationBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var sosListener: ListenerRegistration? = null


    private var senderPhone: String = ""
    private var latitude: Double = 30.0444
    private var longitude: Double = 31.2357

    private var notificationMarker: Marker? = null
    private var firstLocationReceived = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEmergencyNotificationBinding.bind(view)

        setupMiniMap()

        val sosAlertId = arguments?.getString("SOS_ALERT_ID")

        if (!sosAlertId.isNullOrEmpty()) {
            listenToCurrentSOSAlert(sosAlertId)
        } else {
            Toast.makeText(requireContext(), "لم يتم العثور على تفاصيل البلاغ", Toast.LENGTH_SHORT)
                .show()
        }

        binding.btnDismiss.setOnClickListener { findNavController().popBackStack() }

        binding.btnCallUser.setOnClickListener {

            if (senderPhone.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "جاري تحميل رقم الهاتف، يرجى الانتظار...",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$senderPhone"))
                startActivity(intent)
            }
        }

        binding.btnNavigate.setOnClickListener {

            if (latitude != 0.0 && longitude != 0.0) {
                // بدل ما نفتح خرائط جوجل، بنفتح خريطة التطبيق بتاعتنا (MapFragment)
                // ونبعتلها الإحداثيات عشان تحط عليها ماركر مكان البلاغ.
                val locationBundle = bundleOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                findNavController().navigate(R.id.mapFragment, locationBundle)
                val mapIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=$latitude,$longitude")
                )
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                        )
                    )
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "بيانات الموقع لا تزال قيد التحميل...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            })
    }

    /**
     * بتشغّل الخريطة المصغّرة (osmdroid) اللي بتعرض مكان الشخص اللي بعت الـ SOS.
     */
    private fun setupMiniMap() {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        binding.notificationMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.notificationMap.setMultiTouchControls(false)
        binding.notificationMap.controller.setZoom(16.0)
        binding.notificationMap.controller.setCenter(GeoPoint(latitude, longitude))
    }

    private fun updateNotificationMap(lat: Double, lon: Double) {
        if (_binding == null) return
        val point = GeoPoint(lat, lon)

        if (notificationMarker == null) {
            notificationMarker = Marker(binding.notificationMap).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "📍 مكان الاستغاثة"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
            }
            binding.notificationMap.overlays.add(notificationMarker)
        }
        notificationMarker?.position = point

        if (firstLocationReceived) {
            binding.notificationMap.controller.setZoom(16.0)
            firstLocationReceived = false
        }
        binding.notificationMap.controller.animateTo(point)
        binding.notificationMap.invalidate()

        reverseGeocodeAddress(lat, lon)
    }

    private fun reverseGeocodeAddress(lat: Double, lon: Double) {
        thread {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addressLine = addresses?.firstOrNull()?.getAddressLine(0)

                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvLocationTitle.text = addressLine ?: "Lat: %.5f, Lon: %.5f".format(lat, lon)
                    binding.tvLocationSubtitle.text = "📡 تحديث لحظي  •  إشارة GPS جيدة"
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvLocationTitle.text = "Lat: %.5f, Lon: %.5f".format(lat, lon)
                    binding.tvLocationSubtitle.text = "📡 تحديث لحظي"
                }
            }
        }
    }

    private fun listenToCurrentSOSAlert(alertId: String) {
        sosListener = db.collection("emergency_logs")
            .document(alertId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val rawStatus = snapshot.getString("status") ?: "Dispatched"
                val actualStatus = rawStatus.split("|").getOrNull(0) ?: "Dispatched"

                latitude = snapshot.getDouble("latitude") ?: 30.0444
                longitude = snapshot.getDouble("longitude") ?: 31.2357
                updateNotificationMap(latitude, longitude)


                val senderUid = snapshot.getString("userId") ?: ""


                val battery = snapshot.getLong("batteryLevel")?.toInt() ?: 100
                binding.tvLiveInfo.text = "📍 Live Tracking  •  🔋 $battery% Battery"


                val accountName = snapshot.getString("userName") ?: "مستخدم الطوارئ"
                binding.tvSenderName.text = accountName


                if (senderUid.isNotEmpty()) {
                    fetchSenderContactInfo(senderUid, accountName)
                }

                if (actualStatus.contains("Resolved", ignoreCase = true)) {
                    binding.cardAlertIconContainer.setCardBackgroundColor(
                        ColorStateList.valueOf(
                            Color.parseColor("#DCFCE7")
                        )
                    )
                    binding.tvDetailAlertIcon.text = "✅"

                    binding.cardDetailStatusBadge.setCardBackgroundColor(
                        ColorStateList.valueOf(
                            Color.parseColor("#10B981")
                        )
                    )
                    binding.tvDetailStatusText.text = "Resolved / Safe"

                    binding.tvDetailTitle.text = "$accountName أصبحت آمنة الآن 🎉"
                    binding.tvDetailTitle.setTextColor(Color.parseColor("#10B981"))
                    binding.btnNavigate.visibility = View.GONE
                } else {
                    binding.cardAlertIconContainer.setCardBackgroundColor(
                        ColorStateList.valueOf(
                            Color.parseColor("#FEE2E2")
                        )
                    )
                    binding.tvDetailAlertIcon.text = "🚨"

                    binding.cardDetailStatusBadge.setCardBackgroundColor(
                        ColorStateList.valueOf(
                            Color.parseColor("#991B1B")
                        )
                    )
                    binding.tvDetailStatusText.text = "Emergency Alert"

                    binding.tvDetailTitle.text = "إشارة استغاثة نشطة"
                    binding.tvDetailTitle.setTextColor(Color.parseColor("#1E293B"))
                    binding.btnNavigate.visibility = View.VISIBLE
                }
            }
    }


    private fun fetchSenderContactInfo(senderUid: String, accountName: String) {

        db.collection("users").document(senderUid).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc != null && userDoc.exists()) {
                    senderPhone = userDoc.getString("phone") ?: ""

                    if (senderPhone.isNotEmpty() && currentUserId.isNotEmpty()) {

                        db.collection("users")
                            .document(currentUserId)
                            .collection("contacts")
                            .whereEqualTo("phone", senderPhone)
                            .get()
                            .addOnSuccessListener { contactsSnapshot ->
                                if (!contactsSnapshot.isEmpty) {
                                    val contactDoc = contactsSnapshot.documents[0]
                                    val relationship = contactDoc.getString("relationship") ?: ""
                                    val savedName = contactDoc.getString("name") ?: accountName


                                    binding.tvSenderName.text = "$savedName ($relationship)"
                                    binding.tvFallDescription.text =
                                        "ℹ️ بلاغ استغاثة نشط وموثق من صلة القرابة الممسوحة كـ ($relationship) بالحساب: $accountName"
                                } else {

                                    binding.tvFallDescription.text =
                                        "ℹ️ بلاغ استغاثة نشط وموثق للحساب المسجل باسم: $accountName"
                                }
                            }
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        _binding?.notificationMap?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.notificationMap?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sosListener?.remove()
        _binding = null
    }
}