package com.example.saferoute.ui.sensors

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentSensorBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SensorFragment : Fragment(R.layout.fragment_sensor) {

    private var _binding: FragmentSensorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SensorViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {

            binding.switchFallDetection.isChecked = true
            viewModel.startDetection()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel.isDetectionActive.observe(viewLifecycleOwner) { isActive ->
            binding.switchFallDetection.setOnCheckedChangeListener(null)
            binding.switchFallDetection.isChecked = isActive
            binding.switchFallDetection.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkNotificationPermissionAndStart()
                } else {
                    viewModel.stopDetection()
                }
            }
        }

        checkOverlayPermission()
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                binding.switchFallDetection.isChecked = false
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.startDetection()
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${requireContext().packageName}".toUri()
            )
            startActivity(intent)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}