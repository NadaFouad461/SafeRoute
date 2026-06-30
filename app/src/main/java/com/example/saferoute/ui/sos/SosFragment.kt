package com.example.saferoute.ui.sos

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        sosRepository = SosRepository(emergencyRepository)

        viewModel = ViewModelProvider(this, SosViewModelFactory(sosRepository))[SosViewModel::class.java]

        setupHorizontalRecyclerView()


        loadEmergencyContacts()

        binding.btnSos.setOnClickListener {
            if (!isTimerRunning) {
                checkPermissionsAndStart()
            }
        }

        binding.btnCancelSos.setOnClickListener {
            cancelSosCountdown()
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
        db.collection("users")
            .document(currentUserId)
            .collection("contacts")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val contact = ContactItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        isPriority = doc.getBoolean("isPriority") ?: false,
                        fcmToken = doc.getString("fcmToken") ?: ""
                    )
                    emergencyContacts.add(contact)
                }
                sosContactsAdapter.notifyDataSetChanged()


                checkPermissionsAndStart()
            }
            .addOnFailureListener {
                checkPermissionsAndStart()
            }
    }

    private fun checkPermissionsAndStart() {
        if (PermissionManager.hasAllPermissions(requireContext())) {
            startSosCountdown()
        } else {
            requestPermissionsLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS)
        }
    }

    private fun startSosCountdown() {
        if (isTimerRunning) return

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
                if (_binding != null) {
                    binding.tvCountdown.text = "0"
                    isTimerRunning = false

                    binding.progressBar.visibility = View.VISIBLE


                    CoroutineScope(Dispatchers.IO).launch {
                        sosRepository.sendEmergencySos(requireContext().applicationContext, currentUserId)

                        withContext(Dispatchers.Main) {

                            val currentContactsNames = emergencyContacts.map { it.name }
                            val totalContactsCount = currentContactsNames.size

                            val emergencyData = hashMapOf(
                                "userId" to currentUserId,
                                "type" to "SOS",

                                "status" to "Dispatched|$totalContactsCount",
                                "latitude" to 30.3829155,
                                "longitude" to 30.5385578,
                                "alertedContacts" to currentContactsNames,
                                "timestamp" to com.google.firebase.Timestamp.now()
                            )

                            db.collection("emergency_logs")
                                .add(emergencyData)
                                .addOnSuccessListener {
                                    if (_binding != null && isAdded) {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "تم إرسال الاستغاثة بنجاح! 🎉", Toast.LENGTH_LONG).show()
                                        navigateToHistoryFragment()
                                    }
                                }
                                .addOnFailureListener {
                                    if (_binding != null && isAdded) {
                                        binding.progressBar.visibility = View.GONE
                                        navigateToHistoryFragment()
                                    }
                                }
                        }
                    }
                }
            }
        }.start()
    }

    private fun cancelSosCountdown() {
        if (isTimerRunning) {
            countDownTimer?.cancel()
            isTimerRunning = false
            binding.tvCountdown.text = "5"
            Toast.makeText(requireContext(), "تم إلغاء إرسال الاستغاثة بنجاح 🛑", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } else {
            findNavController().navigateUp()
        }
    }

    private fun navigateToHistoryFragment() {
        try {
            findNavController().navigate(R.id.nav_history)
        } catch (e: Exception) {
            try {
                findNavController().navigate(R.id.historyFragment)
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "جاري فتح سجلات الاستغاثة...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}