package com.example.saferoute.ui.sos

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.data.local.AppDatabase
import com.example.saferoute.data.remote.FirestoreService
import com.example.saferoute.data.repository.EmergencyRepository
import com.example.saferoute.data.repository.SosRepository
import com.example.saferoute.databinding.FragmentSosBinding
import com.example.saferoute.models.ContactItem
import com.example.saferoute.utils.PermissionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SosViewModel
    private lateinit var sosRepository: SosRepository
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
    private val db = FirebaseFirestore.getInstance()

    private var emergencyContacts = mutableListOf<ContactItem>()
    private lateinit var sosContactsAdapter: SosContactsAdapter

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false

    private var currentLatitude: Double = 30.0444
    private var currentLongitude: Double = 31.2357


    private var passedSosAlertId: String? = null

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (PermissionManager.hasAllPermissions(requireContext())) {
                startSosCountdown()
            } else {
                Toast.makeText(requireContext(), "يجب الموافقة على الصلاحيات لتشغيل الاستغاثة!", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        passedSosAlertId = arguments?.getString("sosAlertId")

        val dbRoom = AppDatabase.getDatabase(requireContext())
        val firestoreService = FirestoreService()
        val emergencyRepository = EmergencyRepository(dbRoom.emergencyDao(), firestoreService)
        sosRepository = SosRepository(emergencyRepository)

        viewModel = ViewModelProvider(this, SosViewModelFactory(sosRepository))[SosViewModel::class.java]

        setupHorizontalRecyclerView()
        loadEmergencyContacts()

        checkPermissionsAndStart()

        binding.btnSos.setOnClickListener {
            if (!isTimerRunning) {
                checkPermissionsAndStart()
            }
        }

        binding.btnCancelSos.setOnClickListener {
            cancelSosCountdown()
            findNavController().popBackStack()
        }

        binding.btnOpenLogs.setOnClickListener {
            navigateToHistoryFragment()
        }
    }

    private fun setupHorizontalRecyclerView() {
        sosContactsAdapter = SosContactsAdapter(emergencyContacts)
        binding.rvEmergencyContactsHorizontal.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvEmergencyContactsHorizontal.adapter = sosContactsAdapter
    }

    private fun loadEmergencyContacts() {
        emergencyContacts.clear()

        if (currentUserId == "unknown_user") {
            Toast.makeText(requireContext(), "خطأ: لم يتم التعرف على الـ UID للمستخدم الحالي!", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("users")
            .document(currentUserId)
            .collection("contacts")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "⚠️ قائمة جهات الاتصال الطارئة فارغة!", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                var checkCount = 0
                val totalDocs = documents.size()

                for (doc in documents) {
                    val phone = doc.getString("phone") ?: ""
                    val name = doc.getString("name") ?: ""
                    val relationship = doc.getString("relationship") ?: ""
                    val isPriority = doc.getBoolean("isPriority") ?: false

                    db.collection("users")
                        .whereEqualTo("phone", phone)
                        .get()
                        .addOnSuccessListener { userQueryResult ->
                            var realToken = ""
                            if (!userQueryResult.isEmpty) {
                                realToken = userQueryResult.documents[0].getString("fcmToken") ?: ""
                            }

                            val contact = ContactItem(
                                id = doc.id,
                                name = name,
                                phone = phone,
                                relationship = relationship,
                                isPriority = isPriority,
                                fcmToken = realToken
                            )
                            emergencyContacts.add(contact)

                            checkCount++
                            if (checkCount == totalDocs) {
                                sosContactsAdapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener {
                            checkCount++
                            val contact = ContactItem(id = doc.id, name = name, phone = phone, relationship = relationship, isPriority = isPriority, fcmToken = "")
                            emergencyContacts.add(contact)
                            if (checkCount == totalDocs) sosContactsAdapter.notifyDataSetChanged()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "فشل جلب الأرقام: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPermissionsAndStart() {
        if (PermissionManager.hasAllPermissions(requireContext())) {
            startSosCountdown()
        } else {
            requestPermissionsLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS)
        }
    }

    @Synchronized
    private fun startSosCountdown() {
        if (isTimerRunning || countDownTimer != null) {
            return
        }

        isTimerRunning = true
        binding.btnCancelSos.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding != null) {
                    val secondsLeft = (millisUntilFinished / 1000) + 1
                    binding.tvCountdown.text = secondsLeft.toString()
                }
            }

            override fun onFinish() {
                if (_binding == null || !isTimerRunning) return

                binding.tvCountdown.text = "0"
                isTimerRunning = false
                countDownTimer = null
                binding.progressBar.visibility = View.VISIBLE

                val selectedNumbers = emergencyContacts.map { it.phone }
                val message = "🚨 استغاثة طارئة من SafeRoute. الموقع: خط عرض $currentLatitude و خط طول $currentLongitude"


                try {
                    val smsManager = android.telephony.SmsManager.getDefault()
                    for (number in selectedNumbers) {
                        if (number.isNotEmpty()) {
                            smsManager.sendTextMessage(number, null, message, null, null)
                        }
                    }
                } catch (e: Exception) {

                    Log.e("SosFragment", "SMS Error: ${e.message}")
                }


                db.collection("users").document(currentUserId).get()
                    .addOnSuccessListener { userDoc ->
                        val userName = userDoc.getString("name") ?: "مستخدم"
                        val emergencyData = hashMapOf(
                            "userId" to currentUserId,
                            "userName" to userName,
                            "status" to "Emergency Dispatched",
                            "type" to "SOS",
                            "latitude" to currentLatitude,
                            "longitude" to currentLongitude,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("emergency_logs").add(emergencyData).addOnSuccessListener { reference ->
                            val finalDocId = reference.id

                            // إرسال الإشعار
                            emergencyContacts.forEach { contact ->
                                if (contact.fcmToken.isNotEmpty()) {
                                    sendFcmNotification(contact.fcmToken, userName, finalDocId)
                                }
                            }

                            binding.progressBar.visibility = View.GONE
                            findNavController().navigate(R.id.emergencyNotificationFragment, bundleOf("SOS_ALERT_ID" to finalDocId))
                        }
                    }
            }
        }.start()
    }

    private fun sendFcmNotification(token: String, senderName: String, alertId: String) {
        val dataPayload = hashMapOf(
            "title" to "🚨 استغاثة طارئة من $senderName",
            "body" to "الرجاء المساعدة، تم فتح بلاغ طوارئ نشط الآن!",
            "SOS_ALERT_ID" to alertId,
            "senderName" to senderName,
            "click_action" to "EMERGENCY_NOTIFICATION"
        )

        val notificationPayload = hashMapOf(
            "title" to "🚨 استغاثة طارئة من $senderName",
            "body" to "الرجاء المساعدة، تم فتح بلاغ طوارئ نشط الآن!"
        )

        val notificationData = hashMapOf(
            "token" to token,
            "to" to token,
            "priority" to "high",
            "notification" to notificationPayload,
            "data" to dataPayload
        )

        db.collection("notifications_queue").add(notificationData)
    }

    @Synchronized
    private fun cancelSosCountdown() {
        if (countDownTimer != null || isTimerRunning) {
            countDownTimer?.cancel()
            countDownTimer = null
            isTimerRunning = false
        }


        if (!passedSosAlertId.isNullOrEmpty()) {
            db.collection("emergency_logs").document(passedSosAlertId!!)
                .update("status", "canceled")
        }

        if (_binding != null) {
            binding.tvCountdown.text = "5"
            binding.btnCancelSos.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
        }

        Toast.makeText(requireContext(), "تم إلغاء الاستغاثة بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHistoryFragment() {
        try { findNavController().navigate(R.id.emergencyNotificationFragment) } catch(e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}