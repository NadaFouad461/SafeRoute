package com.example.saferoute

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val CHANNEL_ID = "SafeRoute_SOS_Channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName

        // 1. إنشاء قنوات الإشعارات (مطلوب في إصدارات أندرويد الحديثة)
        createNotificationChannel()

        // 2. بدء الاستماع اللحظي لأي طوارئ تحدث في التطبيق
        startListeningForSOSTriggers()
    }

    private fun startListeningForSOSTriggers() {
        // الاستماع لكولكشن emergencies وترتيبها حسب الأحدث
        db.collection("emergencies")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1) // نراقب آخر مستند تم إنشاؤه فقط
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("SOS_LISTENER", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val currentUid = auth.currentUser?.uid ?: return@addSnapshotListener

                // جلب رقم هاتف المستخدم الحالي (الأب أو الأم) لمعرفة هل الاستغاثة موجهة له
                db.collection("users").document(currentUid).get()
                    .addOnSuccessListener { userDocument ->
                        if (userDocument != null && userDocument.exists()) {
                            val myPhone = userDocument.getString("phone") ?: ""

                            for (doc in snapshots!!.documentChanges) {
                                // نتحقق فقط من المستندات المضافة حديثاً (Triggered)
                                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    val emergencyUserId = doc.document.getString("userId")
                                    val userName = doc.document.getString("userName") ?: "Someone"
                                    val locationName = doc.document.getString("locationName") ?: "Unknown Location"

                                    // لمنع هاتف البنت نفسها من استقبال الإشعار الذي أرسلته
                                    if (emergencyUserId != currentUid) {

                                        // 🛑 الفحص الذهبي: نتحقق هل المستغيث يمتلك رقم الهاتف هذا كـ dad أو mom؟
                                        checkIfIAnEmergencyContact(emergencyUserId, myPhone) { isContact ->
                                            if (isContact) {
                                                // إظهار الإشعار فوراً على هاتف الأب/الأم!
                                                showLocalNotification(
                                                    "🚨 SafeRoute EMERGENCY ALERT!",
                                                    "$userName is in danger near $locationName. Check them immediately!"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
    }

    // دالة للتحقق من صلة القرابة في قاعدة البيانات
    private fun checkIfIAnEmergencyContact(emergencyUserId: String?, myPhone: String, callback: (Boolean) -> Unit) {
        if (emergencyUserId == null || myPhone.isEmpty()) {
            callback(false)
            return
        }

        db.collection("users").document(emergencyUserId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val dadPhone = document.getString("dad") ?: ""
                    val momPhone = document.getString("mom") ?: ""

                    // لو رقم هاتف الأب أو الأم الحالي يطابق الأرقام المسجلة عند البنت
                    if (myPhone == dadPhone || myPhone == momPhone) {
                        callback(true)
                    } else {
                        callback(false)
                    }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { callback(false) }
    }

    // دالة بناء وإظهار الإشعار على الشاشة بنجاح
    private fun showLocalNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // يمكنك استبداله بأيقونة تطبيقك المفضلة
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // لعرض الرسالة كاملة لو كانت طويلة

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    // إعداد الـ Channel الخاص بالأندرويد
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Emergency Alerts"
            val descriptionText = "Channels for SafeRoute SOS notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}