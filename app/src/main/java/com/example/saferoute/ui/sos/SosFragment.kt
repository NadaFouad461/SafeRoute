package com.example.saferoute.ui.sos

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.saferoute.data.local.AppDatabase
import com.example.saferoute.data.remote.FirestoreService
import com.example.saferoute.data.repository.EmergencyRepository
import com.example.saferoute.data.repository.SosRepository
import com.example.saferoute.databinding.FragmentSosBinding
import com.example.saferoute.ui.emergency.EmergencyLogActivity
import com.example.saferoute.utils.PermissionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SosViewModel
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
    private val db = FirebaseFirestore.getInstance()

    private var emergencyContacts = mutableListOf<String>()
    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (PermissionManager.hasAllPermissions(requireContext())) {
                startSosCountdown()
            } else {
                Toast.makeText(requireContext(), "يجب الموافقة على الصلاحيات لتشغيل الاستغاثة!", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dbRoom = AppDatabase.getDatabase(requireContext())
        val firestoreService = FirestoreService()
        val emergencyRepository = EmergencyRepository(dbRoom.emergencyDao(), firestoreService)
        val sosRepository = SosRepository(emergencyRepository)

        viewModel = ViewModelProvider(this, SosViewModelFactory(sosRepository))[SosViewModel::class.java]

        // 1. تحميل كافة جهات الاتصال المسجلة في الـ subcollection
        loadEmergencyContacts()

        // 2. تفعيل الـ Countdown عند الضغط على زر الـ SOS لحماية الـ Flow
        binding.btnSos.setOnClickListener {
            if (PermissionManager.hasAllPermissions(requireContext())) {
                startSosCountdown()
            } else {
                requestPermissionsLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS)
            }
        }

        // 3. زر إلغاء الاستغاثة قبل انتهاء العداد
        binding.btnCancelSos.setOnClickListener {
            cancelSosCountdown()
        }

        binding.btnOpenLogs.setOnClickListener {
            startActivity(Intent(requireContext(), EmergencyLogActivity::class.java))
        }

        setupObservers()
    }

    private fun loadEmergencyContacts() {
        emergencyContacts.clear()
        db.collection("users")
            .document(currentUserId)
            .collection("contacts") // قراءة حية وديناميكية من المسار الجديد
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val phone = document.getString("phone")
                    if (!phone.isNullOrEmpty()) {
                        emergencyContacts.add(phone)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "فشل تحميل جهات الاتصال النشطة", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startSosCountdown() {
        if (isTimerRunning) return

        if (emergencyContacts.isEmpty()) {
            Toast.makeText(requireContext(), "من فضلك أضف جهة اتصال طوارئ أولاً في حسابك!", Toast.LENGTH_LONG).show()
            return
        }

        isTimerRunning = true
        binding.btnCancelSos.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountdown.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.tvCountdown.text = "0"
                isTimerRunning = false
                // إطلاق الاستغاثة الفعلية
                viewModel.triggerSos(requireContext(), currentUserId, emergencyContacts)
            }
        }.start()
    }

    private fun cancelSosCountdown() {
        if (isTimerRunning) {
            countDownTimer?.cancel()
            isTimerRunning = false
            binding.tvCountdown.text = "5"
            Toast.makeText(requireContext(), "تم إلغاء إرسال الاستغاثة بنجاح 🛑", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.sosStatus.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SosState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSos.isEnabled = false
                }
                is SosState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSos.isEnabled = true
                    Toast.makeText(requireContext(), "تم إرسال الاستغاثة لكافة الحماة بنجاح! 🎉", Toast.LENGTH_LONG).show()
                }
                is SosState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSos.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}