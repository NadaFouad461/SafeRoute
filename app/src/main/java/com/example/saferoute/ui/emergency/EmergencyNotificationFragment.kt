package com.example.saferoute.ui.emergency

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentEmergencyNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class EmergencyNotificationFragment : Fragment(R.layout.fragment_emergency_notification) {

    private var _binding: FragmentEmergencyNotificationBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var sosListener: ListenerRegistration? = null


    private var senderPhone: String = ""
    private var latitude: Double = 30.0444
    private var longitude: Double = 31.2357

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEmergencyNotificationBinding.bind(view)


        val sosAlertId = arguments?.getString("SOS_ALERT_ID")

        if (!sosAlertId.isNullOrEmpty()) {
            listenToCurrentSOSAlert(sosAlertId)
        } else {
            Toast.makeText(requireContext(), "لم يتم العثور على تفاصيل البلاغ", Toast.LENGTH_SHORT).show()
        }

        binding.btnDismiss.setOnClickListener { findNavController().popBackStack() }

        binding.btnCallUser.setOnClickListener {

            if (senderPhone.isEmpty()) {
                Toast.makeText(requireContext(), "جاري تحميل رقم الهاتف، يرجى الانتظار...", Toast.LENGTH_SHORT).show()
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
            } else {
                Toast.makeText(requireContext(), "بيانات الموقع لا تزال قيد التحميل...", Toast.LENGTH_SHORT).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { findNavController().popBackStack() }
        })
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


                val senderUid = snapshot.getString("userId") ?: ""


                val battery = snapshot.getLong("batteryLevel")?.toInt() ?: 100
                binding.tvLiveInfo.text = "📍 Live Tracking  •  🔋 $battery% Battery"


                val accountName = snapshot.getString("userName") ?: "مستخدم الطوارئ"
                binding.tvSenderName.text = accountName


                if (senderUid.isNotEmpty()) {
                    fetchSenderContactInfo(senderUid, accountName)
                }

                if (actualStatus.contains("Resolved", ignoreCase = true)) {
                    binding.cardAlertIconContainer.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DCFCE7")))
                    binding.tvDetailAlertIcon.text = "✅"

                    binding.cardDetailStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981")))
                    binding.tvDetailStatusText.text = "Resolved / Safe"

                    binding.tvDetailTitle.text = "$accountName أصبحت آمنة الآن 🎉"
                    binding.tvDetailTitle.setTextColor(Color.parseColor("#10B981"))
                    binding.btnNavigate.visibility = View.GONE
                } else {
                    binding.cardAlertIconContainer.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEE2E2")))
                    binding.tvDetailAlertIcon.text = "🚨"

                    binding.cardDetailStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#991B1B")))
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
                                    binding.tvFallDescription.text = "ℹ️ بلاغ استغاثة نشط وموثق من صلة القرابة الممسوحة كـ ($relationship) بالحساب: $accountName"
                                } else {

                                    binding.tvFallDescription.text = "ℹ️ بلاغ استغاثة نشط وموثق للحساب المسجل باسم: $accountName"
                                }
                            }
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sosListener?.remove()
        _binding = null
    }
}