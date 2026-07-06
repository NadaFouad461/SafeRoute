package com.example.saferoute.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationCompat.Builder
import androidx.navigation.fragment.NavHostFragment
import com.example.saferoute.R.drawable
import com.example.saferoute.R.id
import com.example.saferoute.databinding.ActivityMainBinding
import com.example.saferoute.services.FallDetectionService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange.Type
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query.Direction
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import java.security.MessageDigest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val CHANNEL_ID = "SafeRoute_SOS_Channel"

    @Inject
    lateinit var sharedPrefs: SharedPreferences


    private var isTokenUpdated = false

    @RequiresApi(VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().userAgentValue = packageName

        createNotificationChannel()
        startListeningForSOSTriggers()
        checkAndStartSensor()


        if (!isTokenUpdated) {
            updateFcmTokenInFirestore()
        }


        val sosAlertId = intent.getStringExtra("SOS_ALERT_ID")
        if (!sosAlertId.isNullOrEmpty()) {
            val bundle = Bundle().apply {
                putString("SOS_ALERT_ID", sosAlertId)
            }


            binding.root.post {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(id.nav_host_fragment) as? NavHostFragment
                val navController = navHostFragment?.navController
                navController?.navigate(id.emergencyNotificationFragment, bundle)
            }
        }

        handleIncomingNotification(intent)


        try {
            val info =
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signatures = info.signingInfo?.signingCertificateHistory
            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA1")
                    val digest = md.digest(signature.toByteArray())
                    val sha1 = digest.joinToString(":") { String.format("%02X", it) }
                    Log.d("MY_REAL_SHA1", "🎯 الـ SHA-1 الحقيقي لجهازك هو: $sha1")
                }
            }
        } catch (e: Exception) {
            Log.e("MY_REAL_SHA1", "خطأ أثناء استخراج البصمة", e)
        }
    }

    private fun checkAndStartSensor() {
        val isSensorActive = sharedPrefs.getBoolean("IS_FALL_DETECTION_ACTIVE", false)
        if (isSensorActive) {
            val serviceIntent = Intent(this, FallDetectionService::class.java)
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
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
                            Log.e(
                                "FCM_TOKEN_UPDATE",
                                "❌ لم يتم العثور على أي مستند يحتوي على هذا الإيميل!"
                            )
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
        setIntent(intent)

        val sosId = intent.getStringExtra("SOS_ALERT_ID")
        Log.d("NAV_DEBUG", "🚀 onNewIntent - SOS_ID: $sosId")

        if (!sosId.isNullOrEmpty()) {
            val bundle = Bundle().apply {
                putString("SOS_ALERT_ID", sosId)
            }


            binding.root.post {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(id.nav_host_fragment) as? NavHostFragment
                navHostFragment?.navController?.navigate(id.emergencyNotificationFragment, bundle)
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


            binding.root.post {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(id.nav_host_fragment) as? NavHostFragment
                val navController = navHostFragment?.navController
                navController?.navigate(id.historyFragment, bundle)
            }
        }
    }

    private fun startListeningForSOSTriggers() {
        db.collection("emergency_logs")
            .orderBy("timestamp", Direction.DESCENDING)
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
                                if (doc.type == Type.ADDED) {
                                    val logDoc = doc.document
                                    val emergencyUserId = logDoc.getString("userId") ?: ""

                                    if (emergencyUserId != currentUid) {
                                        checkIfIAnEmergencyContact(
                                            emergencyUserId,
                                            myPhone
                                        ) { isContact ->
                                            if (isContact) {
                                                val sosAlertId = logDoc.id


                                                val intent = Intent(
                                                    this@MainActivity,
                                                    MainActivity::class.java
                                                ).apply {
                                                    action = "OPEN_SOS_FRAGMENT"
                                                    val bundle = Bundle()
                                                    bundle.putString("SOS_ALERT_ID", sosAlertId)
                                                    putExtras(bundle)
                                                    flags =
                                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                }


                                                val pendingIntent = PendingIntent.getActivity(
                                                    this@MainActivity,
                                                    0,
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

    private fun checkIfIAnEmergencyContact(
        emergencyUserId: String,
        myPhone: String,
        callback: (Boolean) -> Unit
    ) {
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
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = Builder(this, CHANNEL_ID)
            .setSmallIcon(drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(BigTextStyle().bigText(body))

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val name = "Emergency Alerts"
            val descriptionText = "Channels for SafeRoute SOS notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}