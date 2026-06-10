package com.example.saferoute.ui.sos

import android.content.Intent
import android.os.Bundle
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

class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SosViewModel

    // بيانات تجريبية مؤقتة
    private val currentUserId = "user_123_test"

    private val emergencyContacts = listOf("+201066993026") // رقم تاني غير نفس الجهاز

    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {

            if (PermissionManager.hasAllPermissions(requireContext())) {

                triggerEmergency()

            } else {

                Toast.makeText(
                    requireContext(),
                    "يجب الموافقة على الصلاحيات لتشغيل الاستغاثة!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSosBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        binding.btnSos.setOnClickListener {

            checkPermissionsAndTrigger()

            val intent = Intent(requireContext(), EmergencyLogActivity::class.java)
            startActivity(intent)
        }

        super.onViewCreated(view, savedInstanceState)

        // Room Database
        val db = AppDatabase.getDatabase(requireContext())

        // Firestore Service
        val firestoreService = FirestoreService()

        // Main Repository
        val emergencyRepository = EmergencyRepository(
            db.emergencyDao(),
            firestoreService
        )

        // SOS Repository
        val sosRepository = SosRepository(
            emergencyRepository
        )

        // ViewModel
        viewModel = ViewModelProvider(
            this,
            SosViewModelFactory(sosRepository)
        )[SosViewModel::class.java]

        binding.btnSos.setOnClickListener {

            checkPermissionsAndTrigger()
        }

        setupObservers()
    }

    private fun checkPermissionsAndTrigger() {

        if (PermissionManager.hasAllPermissions(requireContext())) {

            triggerEmergency()

        } else {

            requestPermissionsLauncher.launch(
                PermissionManager.REQUIRED_PERMISSIONS
            )
        }
    }

    private fun triggerEmergency() {

        viewModel.triggerSos(
            requireContext(),
            currentUserId,
            emergencyContacts
        )
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

                    Toast.makeText(
                        requireContext(),
                        "تم إرسال الاستغاثة ومشاركة موقعك بنجاح 🛡️",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is SosState.Error -> {

                    binding.progressBar.visibility = View.GONE
                    binding.btnSos.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()
        _binding = null
    }
}