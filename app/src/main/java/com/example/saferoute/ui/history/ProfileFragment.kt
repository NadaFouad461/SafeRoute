package com.example.saferoute.ui.history

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentProfileBinding
import com.example.saferoute.services.FallDetectionService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val sharedPreferences by lazy {
        requireActivity().getSharedPreferences("SafeRouteSettings", Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)


        fetchUserData()


        loadSettingsState()


        setupClickListeners()


        setupSwitchListeners()
    }

    private fun fetchUserData() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {

            binding.tvProfileName.text = "جاري التحميل... ⏳"
            binding.tvProfileEmail.text = ""

            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (_binding != null && documentSnapshot != null && documentSnapshot.exists()) {

                        val name = documentSnapshot.getString("name") ?: "مستخدم SafeRoute"
                        val email = documentSnapshot.getString("email") ?: ""

                        binding.tvProfileName.text = name
                        binding.tvProfileEmail.text = email
                    }
                }
                .addOnFailureListener {
                    if (_binding != null) {
                        binding.tvProfileName.text = "فشل تحميل الاسم"
                        context?.let { ctx ->
                            Toast.makeText(
                                ctx,
                                "Failed to load updated profile data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }
    }

    private fun loadSettingsState() {
        binding.switchFallDetection.isChecked = sharedPreferences.getBoolean("fall_detection", true)
        binding.switchNotifications.isChecked = sharedPreferences.getBoolean("notifications", true)
        binding.switchDarkMode.isChecked = sharedPreferences.getBoolean("dark_mode", false)
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment2_to_editProfileFragment)
        }


        binding.btnSafeWalkSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Safe Walk Settings...", Toast.LENGTH_SHORT)
                .show()
        }


        binding.btnLanguage.setOnClickListener {
            Toast.makeText(requireContext(), "Language Selection Clicked", Toast.LENGTH_SHORT)
                .show()
        }


        val logoutAction = View.OnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.loginFragment)
        }

        binding.btnLogoutClick.setOnClickListener(logoutAction)
        binding.btnExitApp.setOnClickListener(logoutAction)
    }

    private fun setupSwitchListeners() {
        binding.switchFallDetection.setOnCheckedChangeListener { _, isChecked ->
            // 1. حفظ الحالة بنفس المفتاح المستخدم في باقي التطبيق
            sharedPreferences.edit().putBoolean("IS_FALL_DETECTION_ACTIVE", isChecked).apply()

            // 2. تجهيز الـ Intent اللي بتشاور على خدمة السقوط
            val serviceIntent = Intent(requireContext(), FallDetectionService::class.java)

            if (isChecked) {
                // 3. تشغيل الخدمة في الخلفية
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(serviceIntent)
                } else {
                    requireContext().startService(serviceIntent)
                }
                Toast.makeText(requireContext(), "تم تفعيل مستشعر السقوط", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // 4. إيقاف الخدمة تماماً
                requireContext().stopService(serviceIntent)
                Toast.makeText(requireContext(), "تم إيقاف مستشعر السقوط", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // ... (باقي أزرار الـ Notifications والـ Dark Mode زي ما هي)
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
