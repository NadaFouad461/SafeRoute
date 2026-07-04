package com.example.saferoute.ui.sos

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SosAlertState(
    val senderName: String = "",
    val relation: String = "",
    val avatarUrl: String = "",
    val distance: Double = 0.0,
    val batteryLevel: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val timeAgo: String = "",
    val phoneNumber: String = ""
)

class SosAlertViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _alertState = MutableStateFlow<SosAlertState?>(null)
    val alertState: StateFlow<SosAlertState?> = _alertState

    fun listenToSosAlert(sosAlertId: String) {
        _alertState.value = null

        if (sosAlertId.isEmpty() || sosAlertId == "test_id") return

        firestore.collection("emergency_logs").document(sosAlertId).get()
            .addOnSuccessListener { logSnapshot ->
                if (logSnapshot == null || !logSnapshot.exists()) return@addOnSuccessListener

                val lat = logSnapshot.getDouble("latitude") ?: 0.0
                val lng = logSnapshot.getDouble("longitude") ?: 0.0
                val battery = logSnapshot.getLong("batteryLevel")?.toInt() ?: 100
                val girlUserId = logSnapshot.getString("userId") ?: logSnapshot.getString("girlUserId") ?: ""

                val alertedContacts = logSnapshot.get("alertedContacts") as? List<*> ?: emptyList<Any>()
                val totalAlerted = alertedContacts.size

                // 🎯 القيمة الافتراضية الذكية في حال لم يجد صلة قرابة مخصصة
                var contactRelation = if (totalAlerted > 0) "$totalAlerted Contacts Notified" else "Direct SOS Alert"

                if (girlUserId.isNotEmpty()) {
                    // 1. جلب بيانات البنت (الاسم والرقم)
                    firestore.collection("users").document(girlUserId).get()
                        .addOnSuccessListener { girlSnapshot ->
                            if (!girlSnapshot.exists()) return@addOnSuccessListener

                            val girlRealName = girlSnapshot.getString("name") ?: "مستخدم SafeRoute"
                            val girlPhone = girlSnapshot.getString("phone") ?: ""

                            // 2. 🎯 السحر هنا: البحث برقم البنت جوه جهات اتصال الشخص اللي فاتح الأبليكيشن حالياً (currentUserId)
                            if (currentUserId.isNotEmpty() && girlPhone.isNotEmpty()) {
                                firestore.collection("users").document(currentUserId)
                                    .collection("contacts")
                                    .whereEqualTo("phone", girlPhone)
                                    .get()
                                    .addOnSuccessListener { contactSnapshots ->
                                        var finalDisplayName = girlRealName

                                        // لو الشخص اللي فاتح الموبايل مسجل البنت دي عنده في الـ Contacts
                                        if (!contactSnapshots.isEmpty) {
                                            val contactDoc = contactSnapshots.documents.first()
                                            // الاسم اللي الشخص مسجله عنده (مثلاً: بنتي حبيبتي)
                                            finalDisplayName = contactDoc.getString("name") ?: girlRealName
                                            // 🎯 صلة القرابة الديناميكية (ابنتي / صديقتي / أختي) الحقيقية المحدثة
                                            contactRelation = contactDoc.getString("relation") ?: contactDoc.getString("relationship") ?: contactRelation
                                        }

                                        // تحديث الشاشة بالبيانات المخصصة للشخص اللي بيقرا الإشعار حالياً
                                        _alertState.value = SosAlertState(
                                            senderName = finalDisplayName,
                                            relation = contactRelation, // 🔥 هتظهر "ابنتي" للأم، وتظهر "صديقتي" للصديقة، وتظهر "1 Contacts Notified" لو مش مسجلة!
                                            avatarUrl = "",
                                            distance = 0.0,
                                            batteryLevel = battery,
                                            latitude = lat,
                                            longitude = lng,
                                            address = "مصر، الموقع الحالي للبلاغ",
                                            timeAgo = "نشط الآن",
                                            phoneNumber = girlPhone
                                        )
                                    }
                            }
                        }
                }
            }
    }
}