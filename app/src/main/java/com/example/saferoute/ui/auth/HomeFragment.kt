package com.example.saferoute.ui.auth

import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var currentUserName: String = "SafeRoute User"
    private var currentUserPhone: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // 1. إعداد قائمة الأنشطة الأخيرة
        setupRecentActivityRecyclerView()

        // 2. جلب بيانات المستخدم والترحيب به + الاستماع للبلاغات الطارئة لوالدتك
        fetchUserDataAndGreet()

        // 3. مستمع الضغط لزر جهات الاتصال
        binding.actionContacts.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_contactsListFragment)
        }

        // 4. زر الـ FAB الطارئ الفوري
        binding.fabEmergency.setOnClickListener {
            handleSosTrigger()
        }

        // 5. إضافة الضغط العادي (Click) على كارت الـ SOS لفتح الصفحة فوراً
        binding.sosBtnCard.setOnClickListener {
            handleSosTrigger()
        }

        // 6. الضغط المطول على كارت الـ SOS (كوسيلة حماية إضافية)
        binding.sosBtnCard.setOnLongClickListener {
            handleSosTrigger()
            true
        }

        // 7. شريط التنقل السفلي (Bottom Navigation)
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_map -> {
                    findNavController().navigate(R.id.action_homeFragment_to_mapFragment)
                    true
                }
                R.id.nav_contacts -> {
                    findNavController().navigate(R.id.action_homeFragment_to_contactsListFragment)
                    true
                }

                R.id.nav_history -> {
                    findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
                    true
                }

                R.id.nav_profile -> {
                    findNavController().navigate(R.id.action_homeFragment_to_profileFragment2)
                    true
                }

                else -> false
            }
        }
    }

    // دالة موحدة للتعامل مع تشغيل الـ SOS لمنع تكرار الكود
    private fun handleSosTrigger() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            Toast.makeText(context, "🚨 Sending Instant Emergency Alert...", Toast.LENGTH_SHORT).show()

            // 🔋 جلب نسبة البطارية الحقيقية من نظام الهاتف الآن ديناميكياً
            val batteryManager = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentBatteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            // الإحداثيات الافتراضية للبلاغ (سيتم تحديثها بالخريطة لاحقاً)
            triggerDirectSOS(currentUid, currentUserName, currentBatteryLevel, 30.3346, 31.7504)
        } else {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecentActivityRecyclerView() {
        val activityLogList = listOf(
            HomeItem("1", "🚨 SOS Alert Triggered (SMS Sent)", "Just now", "sos"),
            HomeItem("2", "📞 Fake Call Utility Executed", "15 mins ago", "fake_call"),
            HomeItem("3", "👟 Safe Walk Navigation Completed", "2 hours ago", "safe_walk"),
            HomeItem("4", "📞 Fake Call Scheduled & Received", "Yesterday", "fake_call"),
            HomeItem("5", "🚨 Fall Detection SOS Auto-Triggered", "3 days ago", "sos")
        )

        binding.recentActivityRv.layoutManager = LinearLayoutManager(context)
        binding.recentActivityRv.adapter = HomeAdapter(activityLogList)
    }

    private fun fetchUserDataAndGreet() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (_binding != null && isAdded && documentSnapshot != null && documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("name") ?: "User"
                        currentUserPhone = documentSnapshot.getString("phone") ?: ""
                        binding.welcomeTv.text = "Good Evening, $currentUserName"

                        // 🚨 بمجرد جلب بيانات المستخدم بنجاح، نبدأ بالاستماع لأي بلاغ طوارئ موجه له (لوالدتك مثلاً)
                        startListeningForIncomingSos(currentUid)
                    }
                }
                .addOnFailureListener {
                    if (_binding != null && isAdded) {
                        binding.welcomeTv.text = "Good Evening, Sara"
                    }
                }
        }
    }

    // 🛠️ السحر هنا: دالة الاستماع الفوري للبلاغات الطارئة عند فتح التطبيق
    private fun startListeningForIncomingSos(currentUserId: String) {
        db.collection("emergency_logs")
            .whereEqualTo("status", "triggered")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("HomeFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val sosAlertId = doc.id
                            val girlUserId = doc.getString("userId") ?: ""

                            if (girlUserId == currentUserId) continue

                            // ❌ بدلاً من استدعاء الدالة، ضعي علامة التعليق هذه:
                            // showEmergencyDialog(sosAlertId)
                        }
                    }
                }
            }
    }

    private fun showEmergencyDialog(sosAlertId: String) {
        if (_binding == null || !isAdded) return

        // فحص محلي: لو ماما أغلقت هذا الإشعار مسبقاً لا يظهر لها في الهووم مجدداً
        val sharedPrefs = requireContext().getSharedPreferences("saferoute_prefs", Context.MODE_PRIVATE)
        val isDismissedBefore = sharedPrefs.getBoolean("dismissed_$sosAlertId", false)
        if (isDismissedBefore) return

        AlertDialog.Builder(requireContext())
            .setTitle("🚨 بلاغ استغاثة طارئ SOS!")
            .setMessage("هناك خطر يواجه أحد جهات اتصالك المقربة الآن! اضغطي للانتقال للسجل ومتابعة الحالة.")
            .setCancelable(false)
            .setPositiveButton("الانتقال للسجل (History)") { _, _ ->
                val bundle = Bundle().apply {
                    putString("incomingSosId", sosAlertId)
                    putBoolean("isFromSomeoneElse", true)
                }

                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    navController.navigate(R.id.action_homeFragment_to_historyFragment, bundle)
                }
            }
            .setNegativeButton("إغلاق") { dialog, _ ->
                // 🎯 الحل السحري: حفظ حالة الإغلاق محلياً في جهاز ماما فقط
                // لكي لا يظهر الـ Dialog مرة أخرى، وبنفس الوقت لا نغير الـ status في الفايرستور فلا يختفي من الهيستوري
                sharedPrefs.edit().putBoolean("dismissed_$sosAlertId", true).apply()
                dialog.dismiss()
            }
            .show()
    }

    // 🔋 تم تعديل اسم الجدول هنا ليصبح "emergency_logs" ليتطابق تماماً مع الـ ViewModel
    private fun triggerDirectSOS(userId: String, userName: String, batteryLevel: Int, latitude: Double, longitude: Double) {
        val emergencyData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "status" to "triggered",
            "batteryLevel" to batteryLevel,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "alertedContacts" to listOf<String>() // يمكن ملؤها بأرقام الطوارئ لاحقاً
        )

        // تم التغيير إلى emergency_logs هنا لتوحيد الجداول الثلاثة في قاعدة بياناتك
        db.collection("emergency_logs").add(emergencyData)
            .addOnSuccessListener { documentReference ->
                if (_binding != null && isAdded) {
                    Toast.makeText(context, "🚨 SOS Saved to Database!", Toast.LENGTH_SHORT).show()

                    val bundle = Bundle().apply {
                        putString("sosAlertId", documentReference.id)
                        putInt("batteryLevel", batteryLevel)
                    }

                    try {
                        findNavController().navigate(R.id.action_homeFragment_to_sosFragment, bundle)
                    } catch (e: Exception) {
                        try {
                            findNavController().navigate(R.id.sosFragment, bundle)
                        } catch (navError: Exception) {
                            Toast.makeText(context, "Nav Error: ${navError.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (_binding != null && isAdded) {
                    Toast.makeText(context, "Failed to trigger SOS: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}