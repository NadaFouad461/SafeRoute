package com.example.saferoute.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment
import com.example.saferoute.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.example.saferoute.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val CHANNEL_ID = "SafeRoute_SOS_Channel"

    // المتغير المسؤول عن منع تكرار تحديث التوكن
    private var isTokenUpdated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName

        createNotificationChannel()
        startListeningForSOSTriggers()

        // تحديث التوكن محمي من التكرار
        if (!isTokenUpdated) {
            updateFcmTokenInFirestore()
        }

        // 🔥 تعديل آمن: نقرأ الـ Intent وننتظر حتى يتم تحميل الـ NavHostFragment بالكامل لمنع الكراش
        val sosAlertId = intent.getStringExtra("SOS_ALERT_ID")
        if (!sosAlertId.isNullOrEmpty()) {
            val bundle = Bundle().apply {
                putString("SOS_ALERT_ID", sosAlertId)
            }

            // ننتظر تدوير الـ View للتأكد من أن الـ Navigation Graph جاهز
            binding.root.post {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                val navController = navHostFragment?.navController
                navController?.navigate(R.id.emergencyNotificationFragment, bundle)
            }
        }

        handleIncomingNotification(intent)

        // استخراج الـ SHA-1 الحقيقي للجهاز للتأكد من ربط الفايربيز
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signatures = info.signingInfo?.signingCertificateHistory
            if (signatures != null) {
                for (signature in signatures) {
                    val md = java.security.MessageDigest.getInstance("SHA1")
                    val digest = md.digest(signature.toByteArray())
                    val sha1 = digest.joinToString(":") { String.format("%02X", it) }
                    Log.d("MY_REAL_SHA1", "🎯 الـ SHA-1 الحقيقي لجهازك هو: $sha1")
                }
            }
        } catch (e: Exception) {
            Log.e("MY_REAL_SHA1", "خطأ أثناء استخراج البصمة", e)
        }
    }

    private fun updateFcmTokenInFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userEmail = currentUser.email

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN_UPDATE", "❌ فشل جلب التوكن الجديد", task.exception)
                return@addOnCompleteListener
            }

            val currentToken = task.result
            if (!currentToken.isNullOrEmpty() && !userEmail.isNullOrEmpty()) {
                db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (document in querySnapshot.documents) {
                                db.collection("users").document(document.id)
                                    .update("fcmToken", currentToken)
                                    .addOnSuccessListener {
                                        Log.d("FCM_TOKEN_UPDATE", "✅ تم تحديث التوكن بنجاح!")
                                        isTokenUpdated = true
                                    }
                            }
                        } else {
                            Log.e("FCM_TOKEN_UPDATE", "❌ لم يتم العثور على أي مستند يحتوي على هذا الإيميل!")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM_TOKEN_UPDATE", "❌ فشل الاتصال بقاعدة البيانات", e)
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // مهم جداً لتحديث الـ Intent الخاص بالـ Activity

        val sosId = intent.getStringExtra("SOS_ALERT_ID")
        Log.d("NAV_DEBUG", "🚀 onNewIntent - SOS_ID: $sosId")

        if (!sosId.isNullOrEmpty()) {
            val bundle = Bundle().apply {
                putString("SOS_ALERT_ID", sosId)
            }

            // استخدام post للـ UI Thread
            binding.root.post {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                navHostFragment?.navController?.navigate(R.id.emergencyNotificationFragment, bundle)
            }
        }
    }

    private fun handleIncomingNotification(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("NAVIGATE_TO")
        if (navigateTo == "HISTORY") {
            val sosId = intent.getStringExtra("INCOMING_SOS_ID")
            val senderName = intent.getStringExtra("SENDER_NAME")

            val bundle = Bundle().apply {
                putString("INCOMING_SOS_ID", sosId)
                putString("SENDER_NAME", senderName)
                putBoolean("IS_FROM_SOMEONE_ELSE", true)
            }

            // 🔥 تعديل آمن هنا أيضاً باستخدام الـ View.post
            binding.root.post {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                val navController = navHostFragment?.navController
                navController?.navigate(R.id.historyFragment, bundle)
            }
        }
    }

    private fun startListeningForSOSTriggers() {
        db.collection("emergency_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("SOS_LISTENER", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val currentUid = auth.currentUser?.uid ?: return@addSnapshotListener

                db.collection("users").document(currentUid).get()
                    .addOnSuccessListener { userDocument ->
                        if (userDocument != null && userDocument.exists()) {
                            val myPhone = userDocument.getString("phone") ?: ""

                            for (doc in snapshots!!.documentChanges) {
                                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    val logDoc = doc.document
                                    val emergencyUserId = logDoc.getString("userId") ?: ""

                                    if (emergencyUserId != currentUid) {
                                        checkIfIAnEmergencyContact(emergencyUserId, myPhone) { isContact ->
                                            if (isContact) {
                                                val sosAlertId = logDoc.id

                                                // التعديل: إنشاء Intent يحتوي على البيانات و Flags للتحكم في حالة الـ Activity
                                                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                                                    action = "OPEN_SOS_FRAGMENT"
                                                    val bundle = Bundle()
                                                    bundle.putString("SOS_ALERT_ID", sosAlertId)
                                                    putExtras(bundle)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                }

                                                // استخدام System.currentTimeMillis لضمان رقم فريد لكل إشعار
                                                val pendingIntent = PendingIntent.getActivity(
                                                    this@MainActivity,
                                                    0, // جربي تثبيت الـ RequestCode مؤقتاً لـ 0
                                                    intent,
                                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // غيرناها لـ MUTABLE
                                                )

                                                showLocalNotification(
                                                    "🚨 استغاثة طوارئ SafeRoute!",
                                                    "بنتك في خطر وبحاجة للمساعدة، اضغطي لفتح الموقع حياً",
                                                    pendingIntent
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

    private fun checkIfIAnEmergencyContact(emergencyUserId: String, myPhone: String, callback: (Boolean) -> Unit) {
        if (emergencyUserId.isEmpty() || myPhone.isEmpty()) {
            callback(false)
            return
        }
        db.collection("users").document(emergencyUserId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val dadPhone = document.getString("dad") ?: ""
                    val momPhone = document.getString("mom") ?: ""
                    callback(myPhone == dadPhone || myPhone == momPhone)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { callback(false) }
    }

    private fun showLocalNotification(title: String, body: String, pendingIntent: PendingIntent) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // تأكدي أن الأيقونة موجودة
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL) // مهم جداً للأولويات القصوى
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // 🔥 هذا هو السر: يجبر التطبيق على الفتح
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ليظهر على شاشة القفل
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Emergency Alerts"
            val descriptionText = "Channels for SafeRoute SOS notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}