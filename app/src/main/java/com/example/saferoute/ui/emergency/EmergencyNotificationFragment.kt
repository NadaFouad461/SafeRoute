package com.example.saferoute.ui.emergency

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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

    // متغيرات لتخزين بيانات الموقع والهاتف لإعادة استخدامها عند الضغط على الأزرار
    private var senderPhone: String = ""
    private var latitude: Double = 30.0444
    private var longitude: Double = 31.2357

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEmergencyNotificationBinding.bind(view)

        // استقبال الـ ID بأمان
        val sosAlertId = arguments?.getString("SOS_ALERT_ID")

        if (!sosAlertId.isNullOrEmpty()) {
            listenToCurrentSOSAlert(sosAlertId)
        } else {
            Toast.makeText(context, "لم يتم العثور على تفاصيل البلاغ", Toast.LENGTH_SHORT).show()
        }

        binding.btnDismiss.setOnClickListener { findNavController().popBackStack() }

        binding.btnCallUser.setOnClickListener {
            // التحقق المباشر من النص
            if (senderPhone.isEmpty()) {
                Toast.makeText(context, "جاري تحميل رقم الهاتف، يرجى الانتظار...", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$senderPhone"))
                startActivity(intent)
            }
        }

        binding.btnNavigate.setOnClickListener {
            // التحقق اللحظي من القيم قبل فتح الخرائط
            if (latitude != 0.0 && longitude != 0.0) {
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitude,$longitude"))
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")))
                }
            } else {
                Toast.makeText(context, "بيانات الموقع لا تزال قيد التحميل...", Toast.LENGTH_SHORT).show()
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

                // جلب بيانات الموقع الجغرافي ديناميكياً
                latitude = snapshot.getDouble("latitude") ?: 30.0444
                longitude = snapshot.getDouble("longitude") ?: 31.2357

                // جلب الـ Uid الخاص بالشخص الذي أرسل الـ SOS للبحث عن رقم هاتفه وعلاقته بالمستخدم الحالي
                val senderUid = snapshot.getString("userId") ?: ""

                // 1. تحديث نسبة البطارية الحقيقية للجهاز المرفوع
                val battery = snapshot.getLong("batteryLevel")?.toInt() ?: 100
                binding.tvLiveInfo.text = "📍 Live Tracking  •  🔋 $battery% Battery"

                // 2. جلب اسم الحساب الذي أرسل الاستغاثة
                val accountName = snapshot.getString("userName") ?: "مستخدم الطوارئ"
                binding.tvSenderName.text = accountName

                // 4️⃣ جلب رقم الهاتف وصلة القرابة ديناميكياً من جهات اتصال المستخدم الحالي
                if (senderUid.isNotEmpty()) {
                    fetchSenderContactInfo(senderUid, accountName)
                }

                // 3. التحكم في شكل وحالة البلاغ (نشط / منتهي آمن)
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

    /**
     * دالة تقوم بالبحث في جهات اتصال المستخدم الحالي لمعرفة صلة قرابته بالشخص المستغيث ورقم هاتفه
     */
    private fun fetchSenderContactInfo(senderUid: String, accountName: String) {
        // أولاً: جلب رقم هاتف المرسل من مستنده الأساسي في users
        db.collection("users").document(senderUid).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc != null && userDoc.exists()) {
                    senderPhone = userDoc.getString("phone") ?: ""

                    if (senderPhone.isNotEmpty() && currentUserId.isNotEmpty()) {
                        // ثانياً: البحث عن هذا الرقم داخل قائمة contacts الخاصة بالمستخدم الحالي (ماما مثلاً)
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

                                    // عرض الاسم المسجل مع صلة القرابة (مثال: بنتي (مريم) أو أختي (سارة))
                                    binding.tvSenderName.text = "$savedName ($relationship)"
                                    binding.tvFallDescription.text = "ℹ️ بلاغ استغاثة نشط وموثق من صلة القرابة الممسوحة كـ ($relationship) بالحساب: $accountName"
                                } else {
                                    // إذا لم يكن مسجلاً في جهات الاتصال، نكتفي باسم الحساب فقط
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