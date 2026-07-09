package com.example.saferoute.ui.start

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentPermissionsBinding
import com.example.saferoute.services.FallDetectionService
import com.example.saferoute.ui.sensors.SensorViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionsFragment : Fragment(R.layout.fragment_permissions) {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SensorViewModel by viewModels()


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {

            startFallDetectionSensor()
        } else {
            Toast.makeText(requireContext(), "يجب الموافقة لتفعيل الميزة", Toast.LENGTH_SHORT)
                .show()
            binding.rvPermissions.adapter?.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPermissionsBinding.bind(view)

        val permissionList = listOf(
            PermissionModel(
                title = "Precise Location",
                description = "Required for real-time SOS tracking and sharing your live route with emergency contacts.",
                iconRes = android.R.drawable.ic_menu_mylocation,
                bannerRes = R.drawable.permission_location,
                manifestPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PermissionModel(
                title = "Emergency SMS",
                description = "Allows SafeRoute to automatically send distress messages to your contacts when SOS is triggered.",
                iconRes = R.drawable.permission_emergency_sms,
                bannerRes = R.drawable.boarding_sos,
                manifestPermission = android.Manifest.permission.SEND_SMS
            ),
            PermissionModel(
                title = "Critical Alerts",
                description = "Receive vital safety check-ins, fall detection warnings, and nearby danger zone alerts.",
                iconRes = android.R.drawable.ic_popup_reminder,
                bannerRes = R.drawable.permission_alert,
                manifestPermission = android.Manifest.permission.POST_NOTIFICATIONS
            )
        )

        val adapter = PermissionsAdapter(permissionList) { item, isChecked ->
            item.isGranted = isChecked


            if (item.title == "Critical Alerts") {
                if (isChecked) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        startFallDetectionSensor()
                    }
                } else {
                    stopFallDetectionSensor()
                }
            } else if (item.title == "Precise Location" && isChecked) {
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        binding.rvPermissions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPermissions.adapter = adapter
        if (permissionList.all { it.isGranted }) {
            // 1. تغيير لون الخلفية للأسود
            binding.btnGrantPermissions.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.black)
            )

            // 2. تغيير لون النص للأبيض (عشان يبقى مقروء على الخلفية السودا)
            binding.btnGrantPermissions.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
        }
        binding.btnGrantPermissions.setOnClickListener {
            if (permissionList.all { it.isGranted }) {
                Toast.makeText(requireContext(), "All permissions granted!", Toast.LENGTH_SHORT)
                    .show()
                val sharedPrefs =
                    requireActivity().getSharedPreferences("SafeRoutePrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("isFirstTime", false).apply()
                findNavController().navigate(R.id.action_permissionsFragment_to_loginFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please grant all permissions to proceed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun startFallDetectionSensor() {
        requireActivity().getSharedPreferences("SafeRoutePrefs", Context.MODE_PRIVATE)
            .edit { putBoolean("IS_FALL_DETECTION_ACTIVE", true) }

        val serviceIntent = Intent(requireContext(), FallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }
    }


    private fun stopFallDetectionSensor() {
        requireActivity().getSharedPreferences("SafeRoutePrefs", Context.MODE_PRIVATE)
            .edit { putBoolean("IS_FALL_DETECTION_ACTIVE", false) }

        val serviceIntent = Intent(requireContext(), FallDetectionService::class.java)
        requireContext().stopService(serviceIntent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}